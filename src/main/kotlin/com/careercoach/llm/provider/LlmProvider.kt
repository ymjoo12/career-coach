package com.careercoach.llm.provider

import com.careercoach.llm.dto.*

/**
 * LLM Provider 인터페이스
 * 다양한 LLM 서비스를 통합하기 위한 추상화 계층
 */
interface LlmProvider {
    
    /**
     * 단순 텍스트 생성
     */
    suspend fun generateText(
        prompt: String,
        maxTokens: Int = 2000,
        temperature: Double = 0.7
    ): LlmResponse
    
    /**
     * 구조화된 JSON 응답 생성
     */
    suspend fun generateStructuredResponse(
        prompt: String,
        responseSchema: Map<String, Any>,
        maxTokens: Int = 2000,
        temperature: Double = 0.7
    ): LlmResponse
    
    /**
     * 대화형 응답 생성 (컨텍스트 유지)
     */
    suspend fun generateChatResponse(
        messages: List<ChatMessage>,
        maxTokens: Int = 2000,
        temperature: Double = 0.7
    ): LlmResponse
    
    /**
     * 프로바이더 이름 반환
     */
    fun getProviderName(): String
    
    /**
     * 프로바이더 상태 체크
     */
    suspend fun healthCheck(): Boolean
}