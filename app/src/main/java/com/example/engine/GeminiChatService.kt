package com.example.engine

import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.ChatbotRole
import com.example.data.model.GeminiModelOption
import com.example.data.model.GroundingSource
import com.example.data.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiChatResult(
    val responseText: String,
    val searchQueries: List<String> = emptyList(),
    val sources: List<GroundingSource> = emptyList(),
    val modelUsed: String
)

class GeminiChatService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        history: List<ChatMessage>,
        newPrompt: String,
        modelOption: GeminiModelOption,
        role: ChatbotRole,
        enableSearchGrounding: Boolean,
        bookContext: String? = null
    ): Result<GeminiChatResult> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add your key to the Secrets panel in AI Studio or .env file.")
            )
        }

        val modelName = modelOption.modelId
        val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        try {
            val rootJson = JSONObject()

            // System Instruction with selected role and book context
            val systemSb = StringBuilder(role.systemPrompt)
            if (!bookContext.isNullOrBlank()) {
                systemSb.append("\n\nCurrent Book Context:\n").append(bookContext)
            }

            val systemInstructionObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply { put("text", systemSb.toString()) })
                }
                put("parts", partsArray)
            }
            rootJson.put("systemInstruction", systemInstructionObj)

            // Conversation history (Multi-turn chat)
            val contentsArray = JSONArray()

            // Add previous conversation turns
            history.filter { !it.isError && !it.isLoading }.takeLast(10).forEach { msg ->
                val turnRole = if (msg.sender == MessageSender.USER) "user" else "model"
                val turnObj = JSONObject().apply {
                    put("role", turnRole)
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.text) })
                    }
                    put("parts", parts)
                }
                contentsArray.put(turnObj)
            }

            // Add current user prompt turn
            val currentUserTurn = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", newPrompt) })
                }
                put("parts", parts)
            }
            contentsArray.put(currentUserTurn)
            rootJson.put("contents", contentsArray)

            // Google Search Grounding Tool (enabled for gemini-3.5-flash as mandated)
            if (enableSearchGrounding && modelOption == GeminiModelOption.GENERAL) {
                val toolsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                }
                rootJson.put("tools", toolsArray)
            }

            // Generation config
            val generationConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
            }
            rootJson.put("generationConfig", generationConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = rootJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(endpointUrl)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMessage = try {
                    val errorJson = JSONObject(responseBody)
                    errorJson.optJSONObject("error")?.optString("message") ?: "API Error HTTP ${response.code}"
                } catch (_: Exception) {
                    "API Error HTTP ${response.code}: $responseBody"
                }
                return@withContext Result.failure(Exception(errorMessage))
            }

            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)

            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            val textSb = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i)
                    val partText = part?.optString("text")
                    if (!partText.isNullOrBlank()) {
                        textSb.append(partText)
                    }
                }
            }

            val resultText = textSb.toString().ifBlank { "I have reviewed your inquiry and have no further text." }

            // Extract Google Search Grounding Metadata
            val searchQueries = mutableListOf<String>()
            val sources = mutableListOf<GroundingSource>()

            val groundingMetadata = firstCandidate?.optJSONObject("groundingMetadata")
            if (groundingMetadata != null) {
                val queriesArray = groundingMetadata.optJSONArray("webSearchQueries")
                if (queriesArray != null) {
                    for (i in 0 until queriesArray.length()) {
                        val q = queriesArray.optString(i)
                        if (!q.isNullOrBlank()) searchQueries.add(q)
                    }
                }

                val chunksArray = groundingMetadata.optJSONArray("groundingChunks")
                if (chunksArray != null) {
                    for (i in 0 until chunksArray.length()) {
                        val chunk = chunksArray.optJSONObject(i)
                        val web = chunk?.optJSONObject("web")
                        if (web != null) {
                            val uri = web.optString("uri")
                            val title = web.optString("title").ifBlank { uri }
                            if (uri.isNotBlank()) {
                                sources.add(GroundingSource(title = title, url = uri))
                            }
                        }
                    }
                }
            }

            return@withContext Result.success(
                GeminiChatResult(
                    responseText = resultText,
                    searchQueries = searchQueries,
                    sources = sources,
                    modelUsed = modelOption.displayName
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        }
    }

    /**
     * Translates an excerpt (such as a detected Bangla passage) into a target language
     * using the Gemini 3.5 Flash model.
     */
    suspend fun translateText(
        text: String,
        targetLanguage: String = "English",
        sourceLanguageHint: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add your key to the Secrets panel in AI Studio.")
            )
        }

        val modelName = "gemini-3.5-flash"
        val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        try {
            val rootJson = JSONObject()

            val systemPrompt = "You are an expert multilingual literary translator. Translate the given passage accurately into $targetLanguage while preserving literary eloquence, emotional tone, and nuance. Output ONLY the translated text without introductory phrases, notes, or quotation marks."
            val systemInstructionObj = JSONObject().apply {
                val partsArray = JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                }
                put("parts", partsArray)
            }
            rootJson.put("systemInstruction", systemInstructionObj)

            val sourceDesc = if (!sourceLanguageHint.isNullOrBlank()) " from $sourceLanguageHint" else ""
            val userPrompt = "Translate the following passage$sourceDesc into $targetLanguage:\n\n$text"

            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                    })
                })
            }
            rootJson.put("contents", contentsArray)

            val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(endpointUrl)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Translation failed: HTTP ${response.code} - $responseBody")
                )
            }

            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            val translatedText = if (parts != null && parts.length() > 0) {
                val sb = StringBuilder()
                for (i in 0 until parts.length()) {
                    sb.append(parts.optJSONObject(i)?.optString("text") ?: "")
                }
                sb.toString().trim()
            } else {
                return@withContext Result.failure(Exception("No translation received from Gemini."))
            }

            return@withContext Result.success(translatedText)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        }
    }
}
