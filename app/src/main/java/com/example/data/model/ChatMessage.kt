package com.example.data.model

import java.util.UUID

data class GroundingSource(
    val title: String,
    val url: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = "gemini-3.5-flash",
    val searchQueries: List<String> = emptyList(),
    val sources: List<GroundingSource> = emptyList(),
    val isError: Boolean = false,
    val isLoading: Boolean = false
)

enum class MessageSender {
    USER,
    AI
}

enum class GeminiModelOption(
    val modelId: String,
    val displayName: String,
    val description: String,
    val tag: String
) {
    GENERAL(
        modelId = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        description = "Balanced, versatile & supports Google Search Grounding",
        tag = "Recommended"
    ),
    COMPLEX(
        modelId = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro",
        description = "Deep reasoning, literary critique & complex analysis",
        tag = "Deep Thinking"
    ),
    FAST(
        modelId = "gemini-3.1-flash-lite-preview",
        displayName = "Gemini 3.1 Flash Lite",
        description = "Instant definitions, quick summaries & rapid answers",
        tag = "Ultra Fast"
    )
}

enum class ChatbotRole(
    val roleName: String,
    val roleTitle: String,
    val systemPrompt: String
) {
    LITERARY_SCHOLAR(
        roleName = "Scholar",
        roleTitle = "Literary Scholar & Critic",
        systemPrompt = "You are Tome AI, a distinguished literary scholar, critic, and reading mentor. Provide deep, analytical, and eloquent insights into books, prose style, narrative structure, historical context, and philosophical themes. When analyzing passages, provide thoughtful commentary that enriches the reader's understanding."
    ),
    READING_COMPANION(
        roleName = "Companion",
        roleTitle = "Thoughtful Reading Companion",
        systemPrompt = "You are Tome AI, a friendly, encouraging, and perceptive reading companion. Help the reader understand complex vocabulary, explain plot nuances, clarify confusing paragraphs, and discuss books in an accessible, engaging manner."
    ),
    RESEARCH_ASSISTANT(
        roleName = "Researcher",
        roleTitle = "Grounded Research Assistant",
        systemPrompt = "You are Tome AI, a meticulous research assistant. Utilize Google Search Grounding to provide up-to-date, factually verified information about authors, literary history, contemporary real-world connections, scholarly sources, and book adaptations."
    ),
    SOCRATIC_TUTOR(
        roleName = "Tutor",
        roleTitle = "Socratic Discussion Mentor",
        systemPrompt = "You are Tome AI, a Socratic tutor. Instead of just giving simple answers, guide the reader to think deeply about what they are reading by asking probing questions, examining assumptions, and encouraging active contemplation."
    )
}
