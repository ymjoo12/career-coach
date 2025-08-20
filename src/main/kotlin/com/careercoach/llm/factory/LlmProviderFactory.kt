package com.careercoach.llm.factory

import com.careercoach.llm.provider.LlmProvider
import com.careercoach.llm.provider.GeminiProvider
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * LLM Provider Factory
 * 설정에 따라 적절한 LLM Provider를 생성하고 관리
 */
@Component
class LlmProviderFactory(
    private val geminiProvider: GeminiProvider,
    // 향후 다른 프로바이더들 추가 가능
    // private val openAiProvider: OpenAiProvider,
    // private val claudeProvider: ClaudeProvider,
) {
    private val logger = KotlinLogging.logger {}
    
    /**
     * 프로바이더 이름으로 적절한 구현체 반환
     */
    fun getProvider(providerName: String): LlmProvider {
        logger.debug { "Getting LLM provider: $providerName" }
        
        return when (providerName.lowercase()) {
            "gemini" -> geminiProvider
            // "openai" -> openAiProvider
            // "claude" -> claudeProvider
            else -> {
                logger.warn { "Unknown provider: $providerName, falling back to Gemini" }
                geminiProvider
            }
        }
    }
    
    /**
     * 기본 프로바이더 반환
     */
    fun getDefaultProvider(): LlmProvider = geminiProvider
    
    /**
     * 사용 가능한 모든 프로바이더 목록 반환
     */
    fun getAvailableProviders(): List<String> {
        return listOf("gemini") // 향후 추가될 프로바이더들 포함
    }
    
    /**
     * 모든 프로바이더의 상태 체크
     */
    suspend fun checkAllProvidersHealth(): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()
        
        try {
            results["gemini"] = geminiProvider.healthCheck()
        } catch (e: Exception) {
            logger.error(e) { "Failed to check Gemini provider health" }
            results["gemini"] = false
        }
        
        // 향후 다른 프로바이더들도 체크
        
        return results
    }
}