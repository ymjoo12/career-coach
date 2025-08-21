package com.careercoach.agent.base

import com.careercoach.llm.dto.LlmResponse

interface Agent {
    val name: String
    val description: String
    val specialization: AgentSpecialization
    
    suspend fun process(context: AgentContext): AgentResponse
    fun canHandle(context: AgentContext): Boolean
}

enum class AgentSpecialization {
    INTERVIEW,
    TECHNICAL,
    BEHAVIORAL,
    CAREER_PATH,
    SKILL_ASSESSMENT
}

data class AgentContext(
    val profileId: Long,
    val targetPosition: String? = null,
    val targetCompany: String? = null,
    val requestType: String,
    val parameters: Map<String, Any> = emptyMap(),
    val previousResponses: List<AgentResponse> = emptyList()
)

data class AgentResponse(
    val agentName: String,
    val content: String,
    val metadata: Map<String, Any> = emptyMap(),
    val confidence: Double = 1.0,
    val llmResponse: LlmResponse? = null
)