package com.careercoach.llm.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

/**
 * LLM 응답 DTO
 */
data class LlmResponse(
    val content: String,
    val model: String,
    val usage: TokenUsage,
    val finishReason: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * 토큰 사용량 정보
 */
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * 채팅 메시지
 */
data class ChatMessage(
    val role: MessageRole,
    val content: String
)

/**
 * 메시지 역할
 */
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}

/**
 * LLM 요청 설정
 */
data class LlmConfig(
    val model: String = "gemini-1.5-flash",
    val maxTokens: Int = 2000,
    val temperature: Double = 0.7,
    val topP: Double = 0.95,
    val topK: Int = 40,
    val stopSequences: List<String> = emptyList()
)

/**
 * 프롬프트 템플릿
 */
data class PromptTemplate(
    val name: String,
    val template: String,
    val variables: List<String>,
    val description: String? = null
) {
    fun format(params: Map<String, Any>): String {
        var result = template
        params.forEach { (key, value) ->
            result = result.replace("{$key}", value.toString())
        }
        return result
    }
}

/**
 * LLM 프로바이더 설정
 */
data class LlmProviderConfig(
    val provider: String,
    val apiKey: String? = null,
    val projectId: String? = null,
    val location: String? = null,
    val endpoint: String? = null,
    val timeout: Long = 30000,
    val retryCount: Int = 3
)