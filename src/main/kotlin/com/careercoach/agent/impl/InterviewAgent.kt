package com.careercoach.agent.impl

import com.careercoach.agent.base.*
import com.careercoach.llm.service.LlmService
import com.careercoach.domain.profile.repository.ProfileRepository
import com.careercoach.domain.profile.repository.TechnicalSkillRepository
import com.careercoach.domain.profile.entity.Profile
import com.careercoach.domain.profile.entity.TechnicalSkill
import com.careercoach.llm.dto.LlmResponse
import org.springframework.stereotype.Component

@Component
class InterviewAgent(
    llmService: LlmService,
    profileRepository: ProfileRepository,
    technicalSkillRepository: TechnicalSkillRepository
) : BaseAgent(llmService, profileRepository, technicalSkillRepository) {
    
    override val name = "Interview Agent"
    override val description = "Specializes in generating comprehensive interview questions"
    override val specialization = AgentSpecialization.INTERVIEW
    
    override fun canHandle(context: AgentContext): Boolean {
        return context.requestType in listOf("interview", "interview_questions", "general_interview")
    }
    
    override fun buildPrompt(
        context: AgentContext,
        profile: Profile,
        skills: List<TechnicalSkill>
    ): String {
        val targetPosition = context.targetPosition ?: "백엔드 개발자"
        val targetCompany = context.targetCompany ?: "IT 기업"
        
        return """
            당신은 ${targetCompany}의 시니어 면접관입니다.
            다음 지원자의 프로필을 바탕으로 ${targetPosition} 포지션에 대한 종합적인 면접 질문을 생성해주세요.
            
            지원자 정보:
            - 이름: ${profile.name}
            - 경력: ${profile.yearsOfExperience}년
            - 현재 포지션: ${profile.currentPosition ?: "미정"}
            
            보유 기술:
            ${skills.joinToString("\n") { "- ${it.name}: ${it.level} (${it.yearsOfExperience}년)" }}
            
            주요 경력:
            ${profile.experiences.take(3).joinToString("\n") { 
                "- ${it.company} ${it.position} (${it.startDate}~${it.endDate ?: "현재"})"
            }}
            
            생성할 질문 유형:
            1. 경력 검증 질문 (2개)
            2. 기술 심화 질문 (2개)
            3. 문제 해결 능력 질문 (2개)
            4. 프로젝트 경험 질문 (2개)
            5. 성장 가능성 평가 질문 (1개)
            
            각 질문에는 다음을 포함해주세요:
            - 질문
            - 의도 (무엇을 평가하려는지)
            - 좋은 답변의 키워드
            - 후속 질문 제안
            
            JSON 형식으로 응답해주세요.
        """.trimIndent()
    }
    
    override fun getMaxTokens(): Int = 3000
    override fun getTemperature(): Double = 0.7
    
    override fun extractMetadata(llmResponse: LlmResponse): Map<String, Any> {
        return mapOf(
            "model" to llmResponse.model,
            "tokens_used" to llmResponse.usage.totalTokens,
            "agent_type" to specialization.name
        )
    }
    
    override fun calculateConfidence(llmResponse: LlmResponse): Double {
        return if (llmResponse.content.length > 500) 0.95 else 0.8
    }
    
    override fun getFallbackResponse(context: AgentContext): AgentResponse {
        val fallbackQuestions = """
            {
                "questions": [
                    {
                        "type": "경력 검증",
                        "question": "가장 도전적이었던 프로젝트 경험을 설명해주세요.",
                        "intent": "문제 해결 능력과 기술 깊이 평가",
                        "keywords": ["문제 정의", "해결 과정", "결과", "학습"],
                        "followup": "그 경험에서 가장 큰 교훈은 무엇이었나요?"
                    },
                    {
                        "type": "기술 심화",
                        "question": "현재 사용하시는 기술 스택의 장단점을 설명해주세요.",
                        "intent": "기술 이해도와 비판적 사고 평가",
                        "keywords": ["트레이드오프", "성능", "유지보수", "확장성"],
                        "followup": "다른 대안은 고려해보셨나요?"
                    }
                ]
            }
        """.trimIndent()
        
        return AgentResponse(
            agentName = name,
            content = fallbackQuestions,
            metadata = mapOf("fallback" to true),
            confidence = 0.6
        )
    }
}