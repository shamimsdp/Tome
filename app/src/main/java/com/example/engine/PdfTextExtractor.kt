package com.example.engine

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

/**
 * Lightweight, zero-dependency PDF text extractor.
 * Parses PDF streams (handling FlateDecode compression) and extracts
 * human-readable text from text operators (Tj, TJ, ', ").
 */
object PdfTextExtractor {

    fun extractTextPerPage(file: File, pageCount: Int): Map<Int, String> {
        val resultMap = mutableMapOf<Int, String>()
        try {
            val bytes = file.readBytes()
            val textBlocks = extractAllTextStreams(bytes)

            if (textBlocks.isNotEmpty() && pageCount > 0) {
                // If the number of extracted text streams roughly matches or exceeds page count,
                // distribute them across pages.
                if (textBlocks.size == pageCount) {
                    textBlocks.forEachIndexed { idx, txt ->
                        if (txt.isNotBlank()) resultMap[idx] = txt
                    }
                } else if (textBlocks.size > pageCount) {
                    val ratio = textBlocks.size.toFloat() / pageCount.toFloat()
                    for (p in 0 until pageCount) {
                        val start = (p * ratio).toInt()
                        val end = ((p + 1) * ratio).toInt().coerceAtMost(textBlocks.size)
                        val combined = textBlocks.subList(start, end).joinToString("\n\n").trim()
                        if (combined.isNotBlank()) {
                            resultMap[p] = combined
                        }
                    }
                } else {
                    // Fewer blocks than pages (e.g. single large stream or document)
                    textBlocks.forEachIndexed { idx, txt ->
                        if (txt.isNotBlank()) resultMap[idx] = txt
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resultMap
    }

    private fun extractAllTextStreams(bytes: ByteArray): List<String> {
        val results = mutableListOf<String>()
        var offset = 0
        val len = bytes.size

        while (offset < len) {
            val streamStart = findPattern(bytes, "stream", offset)
            if (streamStart == -1) break

            // Move past "stream\r\n" or "stream\n"
            var dataStart = streamStart + 6
            if (dataStart < len && bytes[dataStart] == '\r'.code.toByte()) dataStart++
            if (dataStart < len && bytes[dataStart] == '\n'.code.toByte()) dataStart++

            val endStream = findPattern(bytes, "endstream", dataStart)
            if (endStream == -1) break

            val streamBytes = bytes.copyOfRange(dataStart, endStream)

            // Look back before streamStart to check if /FlateDecode was specified
            val headerSearchStart = (streamStart - 250).coerceAtLeast(0)
            val headerString = String(bytes.copyOfRange(headerSearchStart, streamStart), StandardCharsets.ISO_8859_1)
            val isFlate = headerString.contains("/FlateDecode") || headerString.contains("/Fl")

            val decompressedBytes = if (isFlate) {
                decompressFlate(streamBytes)
            } else {
                streamBytes
            }

            if (decompressedBytes != null && decompressedBytes.isNotEmpty()) {
                val extractedText = parseTextOperators(decompressedBytes)
                if (extractedText.isNotBlank()) {
                    results.add(extractedText)
                }
            }

            offset = endStream + 9
        }

        return results
    }

    private fun decompressFlate(data: ByteArray): ByteArray? {
        return try {
            val bis = ByteArrayInputStream(data)
            val iis = InflaterInputStream(bis)
            iis.readBytes()
        } catch (_: Exception) {
            try {
                // Retry with raw Inflater (nowrap = true)
                val inflater = Inflater(true)
                inflater.setInput(data)
                val buffer = ByteArray(4096)
                val output = java.io.ByteArrayOutputStream()
                while (!inflater.finished()) {
                    val count = inflater.inflate(buffer)
                    if (count <= 0) break
                    output.write(buffer, 0, count)
                }
                inflater.end()
                output.toByteArray()
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun parseTextOperators(bytes: ByteArray): String {
        val content = String(bytes, StandardCharsets.ISO_8859_1)
        val sb = StringBuilder()

        var inTextObject = false
        var i = 0
        val n = content.length

        while (i < n) {
            // Check BT (Begin Text)
            if (!inTextObject && i + 1 < n && content[i] == 'B' && content[i + 1] == 'T' && isWhitespaceOrBoundary(content, i, 2)) {
                inTextObject = true
                i += 2
                continue
            }

            // Check ET (End Text)
            if (inTextObject && i + 1 < n && content[i] == 'E' && content[i + 1] == 'T' && isWhitespaceOrBoundary(content, i, 2)) {
                inTextObject = false
                sb.append("\n")
                i += 2
                continue
            }

            if (inTextObject) {
                // Check literal string ( ... ) Tj
                if (content[i] == '(') {
                    val strEnd = findMatchingParen(content, i)
                    if (strEnd != -1) {
                        val rawStr = content.substring(i + 1, strEnd)
                        val afterStr = content.substring(strEnd + 1, (strEnd + 15).coerceAtMost(n)).trimStart()
                        if (afterStr.startsWith("Tj") || afterStr.startsWith("'") || afterStr.startsWith("\"")) {
                            sb.append(decodePdfString(rawStr)).append(" ")
                        }
                        i = strEnd + 1
                        continue
                    }
                }

                // Check hex string < ... > Tj
                if (content[i] == '<' && (i + 1 < n && content[i + 1] != '<')) {
                    val hexEnd = content.indexOf('>', i)
                    if (hexEnd != -1) {
                        val rawHex = content.substring(i + 1, hexEnd).trim()
                        val afterStr = content.substring(hexEnd + 1, (hexEnd + 15).coerceAtMost(n)).trimStart()
                        if (afterStr.startsWith("Tj")) {
                            sb.append(decodeHexPdfString(rawHex)).append(" ")
                        }
                        i = hexEnd + 1
                        continue
                    }
                }

                // Check array [ ... ] TJ
                if (content[i] == '[') {
                    val arrayEnd = content.indexOf(']', i)
                    if (arrayEnd != -1) {
                        val arrayContent = content.substring(i + 1, arrayEnd)
                        val afterArray = content.substring(arrayEnd + 1, (arrayEnd + 15).coerceAtMost(n)).trimStart()
                        if (afterArray.startsWith("TJ")) {
                            sb.append(parseTJArray(arrayContent)).append(" ")
                        }
                        i = arrayEnd + 1
                        continue
                    }
                }

                // Check newline operator T*
                if (i + 1 < n && content[i] == 'T' && content[i + 1] == '*') {
                    sb.append("\n")
                    i += 2
                    continue
                }
            }

            i++
        }

        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun findMatchingParen(str: String, startIndex: Int): Int {
        var depth = 1
        var idx = startIndex + 1
        val len = str.length
        var escaped = false

        while (idx < len) {
            val c = str[idx]
            if (escaped) {
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == '(') {
                depth++
            } else if (c == ')') {
                depth--
                if (depth == 0) return idx
            }
            idx++
        }
        return -1
    }

    private fun parseTJArray(arrayStr: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = arrayStr.length

        while (i < len) {
            if (arrayStr[i] == '(') {
                val end = findMatchingParen(arrayStr, i)
                if (end != -1) {
                    sb.append(decodePdfString(arrayStr.substring(i + 1, end)))
                    i = end + 1
                    continue
                }
            } else if (arrayStr[i] == '<' && (i + 1 < len && arrayStr[i + 1] != '<')) {
                val end = arrayStr.indexOf('>', i)
                if (end != -1) {
                    sb.append(decodeHexPdfString(arrayStr.substring(i + 1, end)))
                    i = end + 1
                    continue
                }
            }
            i++
        }
        return sb.toString()
    }

    private fun decodePdfString(s: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = s.length
        while (i < len) {
            val c = s[i]
            if (c == '\\' && i + 1 < len) {
                val next = s[i + 1]
                when (next) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    '(', ')', '\\' -> sb.append(next)
                    in '0'..'7' -> {
                        // Octal
                        val octal = s.substring(i + 1, (i + 4).coerceAtMost(len)).takeWhile { it in '0'..'7' }
                        val code = octal.toIntOrNull(8) ?: next.code
                        sb.append(code.toChar())
                        i += octal.length
                        continue
                    }
                    else -> sb.append(next)
                }
                i += 2
                continue
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    private fun decodeHexPdfString(hex: String): String {
        val clean = hex.replace(Regex("\\s+"), "")
        val bytes = ByteArray(clean.length / 2)
        for (i in bytes.indices) {
            val index = i * 2
            bytes[i] = clean.substring(index, index + 2).toIntOrNull(16)?.toByte() ?: 0
        }
        // Check for UTF-16BE BOM (FE FF)
        return if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            String(bytes.copyOfRange(2, bytes.size), StandardCharsets.UTF_16BE)
        } else {
            String(bytes, StandardCharsets.ISO_8859_1)
        }
    }

    private fun isWhitespaceOrBoundary(str: String, index: Int, tokenLen: Int): Boolean {
        val beforeOk = index == 0 || str[index - 1].isWhitespace()
        val afterIdx = index + tokenLen
        val afterOk = afterIdx >= str.length || str[afterIdx].isWhitespace()
        return beforeOk && afterOk
    }

    private fun findPattern(src: ByteArray, pattern: String, fromIndex: Int): Int {
        val patBytes = pattern.toByteArray(StandardCharsets.ISO_8859_1)
        val patLen = patBytes.size
        val max = src.size - patLen

        for (i in fromIndex..max) {
            var found = true
            for (j in 0 until patLen) {
                if (src[i + j] != patBytes[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }
}
