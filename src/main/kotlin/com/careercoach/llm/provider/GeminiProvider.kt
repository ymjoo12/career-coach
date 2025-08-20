package com.careercoach.llm.provider

import com.careercoach.llm.dto.*
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Google Gemini API Provider 구현체
 */
@Component
class GeminiProvider(
    private val objectMapper: ObjectMapper,
    @Value("\${llm.gemini.api-key:}") private val apiKey: String,
    @Value("\${llm.gemini.model:gemini-1.5-flash}") private val defaultModel: String,
    @Value("\${llm.gemini.endpoint:https://generativelanguage.googleapis.com/v1beta}") private val endpoint: String,
    @Value("\${llm.gemini.timeout:30000}") private val timeout: Long
) : LlmProvider {
    
    private val logger = KotlinLogging.logger {}
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(timeout, TimeUnit.MILLISECONDS)
        .readTimeout(timeout, TimeUnit.MILLISECONDS)
        .writeTimeout(timeout, TimeUnit.MILLISECONDS)
        .build()
    
    private val mediaType = "application/json".toMediaType()
    
    override suspend fun generateText(
        prompt: String,
        maxTokens: Int,
        temperature: Double
    ): LlmResponse = withContext(Dispatchers.IO) {
        logger.debug { "Generating text with Gemini API" }
        
        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to temperature,
                "maxOutputTokens" to maxTokens,
                "topP" to 0.95,
                "topK" to 40
            )
        )
        
        val response = callGeminiApi(requestBody)
        parseGeminiResponse(response)
    }
    
    override suspend fun generateStructuredResponse(
        prompt: String,
        responseSchema: Map<String, Any>,
        maxTokens: Int,
        temperature: Double
    ): LlmResponse = withContext(Dispatchers.IO) {
        logger.debug { "Generating structured response with Gemini API" }
        
        val enhancedPrompt = """
            $prompt
            
            Please respond with a valid JSON object that follows this schema:
            ${objectMapper.writeValueAsString(responseSchema)}
            
            Respond ONLY with the JSON object, no additional text.
        """.trimIndent()
        
        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to enhancedPrompt)
                    )
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to temperature,
                "maxOutputTokens" to maxTokens,
                "topP" to 0.95,
                "topK" to 40,
                "responseMimeType" to "application/json"
            )
        )
        
        val response = callGeminiApi(requestBody)
        parseGeminiResponse(response)
    }
    
    override suspend fun generateChatResponse(
        messages: List<ChatMessage>,
        maxTokens: Int,
        temperature: Double
    ): LlmResponse = withContext(Dispatchers.IO) {
        logger.debug { "Generating chat response with Gemini API" }
        
        val contents = messages.map { message ->
            mapOf(
                "role" to when (message.role) {
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "model"
                    MessageRole.SYSTEM -> "user" // Gemini doesn't have system role
                },
                "parts" to listOf(
                    mapOf("text" to message.content)
                )
            )
        }
        
        val requestBody = mapOf(
            "contents" to contents,
            "generationConfig" to mapOf(
                "temperature" to temperature,
                "maxOutputTokens" to maxTokens,
                "topP" to 0.95,
                "topK" to 40
            )
        )
        
        val response = callGeminiApi(requestBody)
        parseGeminiResponse(response)
    }
    
    override fun getProviderName(): String = "Gemini"
    
    override suspend fun healthCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = generateText("Hello", maxTokens = 10, temperature = 0.1)
            response.content.isNotEmpty()
        } catch (e: Exception) {
            logger.error(e) { "Health check failed for Gemini provider" }
            false
        }
    }
    
    private fun callGeminiApi(requestBody: Map<String, Any>): String {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is not configured")
        }
        
        val json = objectMapper.writeValueAsString(requestBody)
        val body = json.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("$endpoint/models/$defaultModel:generateContent?key=$apiKey")
            .post(body)
            .build()
        
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()
            logger.error { "Gemini API error: ${response.code} - $errorBody" }
            throw IOException("Gemini API call failed: ${response.code}")
        }
        
        return response.body?.string() ?: throw IOException("Empty response from Gemini API")
    }
    
    private fun parseGeminiResponse(responseJson: String): LlmResponse {
        val response = objectMapper.readTree(responseJson)
        
        val candidate = response["candidates"]?.get(0)
            ?: throw IOException("No candidates in Gemini response")
        
        val content = candidate["content"]?.get("parts")?.get(0)?.get("text")?.asText()
            ?: throw IOException("No content in Gemini response")
        
        val usageMetadata = response["usageMetadata"]
        val promptTokens = usageMetadata?.get("promptTokenCount")?.asInt() ?: 0
        val completionTokens = usageMetadata?.get("candidatesTokenCount")?.asInt() ?: 0
        
        val finishReason = candidate["finishReason"]?.asText()
        
        return LlmResponse(
            content = content,
            model = defaultModel,
            usage = TokenUsage(
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                totalTokens = promptTokens + completionTokens
            ),
            finishReason = finishReason
        )
    }
}