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
class BehavioralAgent(
    llmService: LlmService,
    profileRepository: ProfileRepository,
    technicalSkillRepository: TechnicalSkillRepository
) : BaseAgent(llmService, profileRepository, technicalSkillRepository) {
    
    override val name = "Behavioral Agent"
    override val description = "Specializes in behavioral and cultural fit assessments"
    override val specialization = AgentSpecialization.BEHAVIORAL
    
    override fun canHandle(context: AgentContext): Boolean {
        return context.requestType in listOf("behavioral", "culture_fit", "soft_skills", "leadership")
    }
    
    override fun buildPrompt(
        context: AgentContext,
        profile: Profile,
        skills: List<TechnicalSkill>
    ): String {
        val targetPosition = context.targetPosition ?: "개발자"
        val targetCompany = context.targetCompany ?: "IT 기업"
        val seniorityLevel = when (profile.yearsOfExperience) {
            in 0..2 -> "주니어"
            in 3..5 -> "미드레벨"
            in 6..9 -> "시니어"
            else -> "리드/프린시플"
        }
        
        return """
            당신은 ${targetCompany}의 인사 담당자이자 행동 면접 전문가입니다.
            ${seniorityLevel} ${targetPosition} 지원자를 위한 행동 기반 면접 질문을 생성해주세요.
            
            지원자 정보:
            - 이름: ${profile.name}
            - 경력: ${profile.yearsOfExperience}년
            - 현재 포지션: ${profile.currentPosition ?: "미정"}
            
            경력 배경:
            ${profile.experiences.take(3).joinToString("\n") { 
                "- ${it.company}: ${it.position} (${it.startDate}~${it.endDate ?: "현재"})"
            }}
            
            생성할 질문 카테고리:
            
            1. 리더십 & 팀워크 (3문제)
               - 협업 경험
               - 갈등 해결
               - 멘토링/코칭
            
            2. 문제 해결 & 의사결정 (3문제)
               - 어려운 상황 극복
               - 우선순위 설정
               - 리스크 관리
            
            3. 커뮤니케이션 (2문제)
               - 이해관계자 소통
               - 기술적 내용 설명
               - 피드백 주고받기
            
            4. 성장 마인드셋 (2문제)
               - 실패 경험과 학습
               - 자기계발
               - 커리어 목표
            
            5. 기업 문화 적합성 (2문제)
               - 일하는 방식
               - 가치관
               - 조직 기여
            
            각 질문은 STAR(Situation, Task, Action, Result) 형식으로 답변할 수 있도록 구성하고,
            다음을 포함해주세요:
            - 질문 내용
            - 평가하려는 역량
            - 좋은 답변의 특징
            - 레드 플래그 (주의할 답변)
            - 후속 질문
            
            JSON 형식으로 응답해주세요.
        """.trimIndent()
    }
    
    override fun getMaxTokens(): Int = 3500
    override fun getTemperature(): Double = 0.8
    
    override fun extractMetadata(llmResponse: LlmResponse): Map<String, Any> {
        return mapOf(
            "model" to llmResponse.model,
            "tokens_used" to llmResponse.usage.totalTokens,
            "agent_type" to specialization.name,
            "focus_areas" to listOf("leadership", "teamwork", "communication", "growth_mindset")
        )
    }
    
    override fun calculateConfidence(llmResponse: LlmResponse): Double {
        val hasStructure = llmResponse.content.contains("STAR") || 
                          llmResponse.content.contains("behavioral")
        return if (hasStructure && llmResponse.content.length > 800) 0.92 else 0.75
    }
    
    override fun getFallbackResponse(context: AgentContext): AgentResponse {
        val fallbackQuestions = """
            {
                "behavioral_questions": [
                    {
                        "category": "리더십 & 팀워크",
                        "question": "팀 내 의견 충돌이 있었던 상황과 해결 과정을 설명해주세요.",
                        "competency": "갈등 해결, 협업 능력",
                        "good_answer_traits": [
                            "구체적인 상황 설명",
                            "다양한 관점 인정",
                            "중재자 역할",
                            "Win-Win 해결책",
                            "배운 점 언급"
                        ],
                        "red_flags": [
                            "책임 회피",
                            "일방적인 비난",
                            "감정적 대응",
                            "해결 과정 부재"
                        ],
                        "followup": "그 경험이 이후 팀워크에 어떤 영향을 미쳤나요?"
                    },
                    {
                        "category": "문제 해결",
                        "question": "제한된 리소스로 급한 프로젝트를 완료해야 했던 경험을 공유해주세요.",
                        "competency": "우선순위 설정, 창의적 문제 해결",
                        "good_answer_traits": [
                            "명확한 우선순위",
                            "창의적 해결책",
                            "리스크 관리",
                            "이해관계자 소통",
                            "결과 측정"
                        ],
                        "red_flags": [
                            "무계획적 접근",
                            "품질 무시",
                            "단독 결정",
                            "실패 미인정"
                        ],
                        "followup": "비슷한 상황을 다시 마주한다면 어떻게 다르게 접근하시겠습니까?"
                    },
                    {
                        "category": "성장 마인드셋",
                        "question": "커리어에서 가장 큰 실패와 그로부터 배운 점을 말씀해주세요.",
                        "competency": "자기 인식, 학습 능력, 회복탄력성",
                        "good_answer_traits": [
                            "정직한 실패 인정",
                            "원인 분석",
                            "구체적 개선 행동",
                            "성장으로 연결",
                            "긍정적 마인드"
                        ],
                        "red_flags": [
                            "실패 부인",
                            "남 탓하기",
                            "학습 부재",
                            "반복된 실수"
                        ],
                        "followup": "그 교훈을 최근에 어떻게 적용하셨나요?"
                    }
                ]
            }
        """.trimIndent()
        
        return AgentResponse(
            agentName = name,
            content = fallbackQuestions,
            metadata = mapOf("fallback" to true),
            confidence = 0.55
        )
    }
}