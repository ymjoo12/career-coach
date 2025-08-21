package com.careercoach.agent.base

import com.careercoach.llm.service.LlmService
import com.careercoach.domain.profile.repository.ProfileRepository
import com.careercoach.domain.profile.repository.TechnicalSkillRepository
import com.careercoach.common.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory

abstract class BaseAgent(
    protected val llmService: LlmService,
    protected val profileRepository: ProfileRepository,
    protected val technicalSkillRepository: TechnicalSkillRepository
) : Agent {
    
    protected val logger = LoggerFactory.getLogger(this::class.java)
    
    override suspend fun process(context: AgentContext): AgentResponse {
        if (!canHandle(context)) {
            throw IllegalArgumentException("Agent $name cannot handle this context")
        }
        
        val profile = profileRepository.findByIdWithExperiences(context.profileId)
            .orElseThrow { ResourceNotFoundException("Profile not found with id: ${context.profileId}") }
        
        val skills = technicalSkillRepository.findByProfileId(context.profileId)
        
        return try {
            val prompt = buildPrompt(context, profile, skills)
            val llmResponse = llmService.generateText(
                prompt = prompt,
                maxTokens = getMaxTokens(),
                temperature = getTemperature()
            )
            
            AgentResponse(
                agentName = name,
                content = llmResponse.content,
                metadata = extractMetadata(llmResponse),
                confidence = calculateConfidence(llmResponse),
                llmResponse = llmResponse
            )
        } catch (e: Exception) {
            logger.error("Error in $name agent processing", e)
            getFallbackResponse(context)
        }
    }
    
    protected abstract fun buildPrompt(
        context: AgentContext,
        profile: com.careercoach.domain.profile.entity.Profile,
        skills: List<com.careercoach.domain.profile.entity.TechnicalSkill>
    ): String
    
    protected abstract fun getMaxTokens(): Int
    protected abstract fun getTemperature(): Double
    protected abstract fun extractMetadata(llmResponse: com.careercoach.llm.dto.LlmResponse): Map<String, Any>
    protected abstract fun calculateConfidence(llmResponse: com.careercoach.llm.dto.LlmResponse): Double
    protected abstract fun getFallbackResponse(context: AgentContext): AgentResponse
}