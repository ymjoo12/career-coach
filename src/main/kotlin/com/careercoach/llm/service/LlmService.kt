package com.careercoach.llm.service

import com.careercoach.llm.dto.*
import com.careercoach.llm.factory.LlmProviderFactory
import com.careercoach.llm.provider.LlmProvider
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * LLM 통합 서비스
 * 프로바이더 추상화 및 캐싱, 에러 처리 등을 담당
 */
@Service
class LlmService(
    private val providerFactory: LlmProviderFactory,
    private val promptTemplateService: PromptTemplateService,
    private val objectMapper: ObjectMapper,
    @Value("\${llm.default-provider:gemini}") private val defaultProviderName: String
) {
    
    private val logger = KotlinLogging.logger {}
    
    /**
     * 템플릿 기반 텍스트 생성
     */
    fun generateFromTemplate(
        templateName: String,
        params: Map<String, Any>,
        providerName: String? = null,
        maxTokens: Int = 2000,
        temperature: Double = 0.7
    ): LlmResponse {
        val prompt = promptTemplateService.generatePrompt(templateName, params)
        return generateText(prompt, providerName, maxTokens, temperature)
    }
    
    /**
     * 단순 텍스트 생성
     */
    @Cacheable(value = ["llm-text"], key = "#prompt + #providerName + #maxTokens + #temperature")
    fun generateText(
        prompt: String,
        providerName: String? = null,
        maxTokens: Int = 2000,
        temperature: Double = 0.7
    ): LlmResponse {
        logger.debug { "Generating text with provider: ${providerName ?: defaultProviderName}" }
        
        val provider = getProvider(providerName)
        
        return runBlocking {
            try {
                provider.generateText(prompt, maxTokens, temperature)
            } catch (e: Exception) {
                logger.error(e) { "Failed to generate text" }
                handleError(e, provider)
            }
        }
    }
    
    /**
     * 구조화된 JSON 응답 생성
     */
    @Cacheable(value = ["llm-structured"], key = "#prompt + #responseSchema.hashCode() + #providerName")
    fun generateStructuredResponse(
        prompt: String,
        responseSchema: Map<String, Any>,
        providerName: String? = null,
        maxTokens: Int = 2000,
        temperature: Double = 0.7
    ): LlmResponse {
        logger.debug { "Generating structured response" }
        
        val provider = getProvider(providerName)
        
        return runBlocking {
            try {
                val response = provider.generateStructuredResponse(
                    prompt, responseSchema, maxTokens, temperature
                )
                
                // JSON 유효성 검증
                validateJsonResponse(response.content, responseSchema)
                
                response
            } catch (e: Exception) {
                logger.error(e) { "Failed to generate structured response" }
                handleError(e, provider)
            }
        }
    }
    
    /**
     * 대화형 응답 생성
     */
    fun generateChatResponse(
        messages: List<ChatMessage>,
        providerName: String? = null,
        maxTokens: Int = 2000,
        temperature: Double = 0.7
    ): LlmResponse {
        logger.debug { "Generating chat response with ${messages.size} messages" }
        
        val provider = getProvider(providerName)
        
        return runBlocking {
            try {
                provider.generateChatResponse(messages, maxTokens, temperature)
            } catch (e: Exception) {
                logger.error(e) { "Failed to generate chat response" }
                handleError(e, provider)
            }
        }
    }
    
    /**
     * 프로바이더 상태 체크
     */
    fun checkProviderHealth(providerName: String? = null): Boolean {
        val provider = getProvider(providerName)
        
        return runBlocking {
            try {
                provider.healthCheck()
            } catch (e: Exception) {
                logger.error(e) { "Provider health check failed: ${provider.getProviderName()}" }
                false
            }
        }
    }
    
    /**
     * 모든 프로바이더 상태 체크
     */
    fun checkAllProvidersHealth(): Map<String, Boolean> {
        return runBlocking {
            providerFactory.checkAllProvidersHealth()
        }
    }
    
    /**
     * 사용 가능한 프로바이더 목록
     */
    fun getAvailableProviders(): List<String> {
        return providerFactory.getAvailableProviders()
    }
    
    /**
     * 프로바이더 가져오기
     */
    private fun getProvider(providerName: String?): LlmProvider {
        return if (providerName != null) {
            providerFactory.getProvider(providerName)
        } else {
            providerFactory.getProvider(defaultProviderName)
        }
    }
    
    /**
     * JSON 응답 유효성 검증
     */
    private fun validateJsonResponse(content: String, schema: Map<String, Any>) {
        try {
            val jsonNode = objectMapper.readTree(content)
            // 기본적인 스키마 검증 (필수 필드 존재 여부 등)
            schema.forEach { (key, _) ->
                if (!jsonNode.has(key)) {
                    logger.warn { "Missing required field in JSON response: $key" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Invalid JSON response: $content" }
            throw IllegalStateException("Invalid JSON response from LLM", e)
        }
    }
    
    /**
     * 에러 처리 및 폴백
     */
    private suspend fun handleError(error: Exception, provider: LlmProvider): LlmResponse {
        logger.error { "LLM error with provider ${provider.getProviderName()}: ${error.message}" }
        
        // 다른 프로바이더로 폴백 시도 (향후 구현)
        // val fallbackProvider = getFallbackProvider(provider)
        // if (fallbackProvider != null) {
        //     return fallbackProvider.generateText(...)
        // }
        
        throw error
    }
    
    /**
     * 토큰 사용량 통계
     */
    fun getTokenUsageStats(): Map<String, Any> {
        // 향후 구현: 토큰 사용량 추적 및 통계
        return mapOf(
            "totalTokens" to 0,
            "totalCost" to 0.0,
            "byProvider" to mapOf<String, Any>()
        )
    }
}