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
class TechnicalAgent(
    llmService: LlmService,
    profileRepository: ProfileRepository,
    technicalSkillRepository: TechnicalSkillRepository
) : BaseAgent(llmService, profileRepository, technicalSkillRepository) {
    
    override val name = "Technical Agent"
    override val description = "Specializes in deep technical assessments and coding challenges"
    override val specialization = AgentSpecialization.TECHNICAL
    
    override fun canHandle(context: AgentContext): Boolean {
        return context.requestType in listOf("technical", "coding", "algorithm", "system_design")
    }
    
    override fun buildPrompt(
        context: AgentContext,
        profile: Profile,
        skills: List<TechnicalSkill>
    ): String {
        val targetPosition = context.targetPosition ?: "백엔드 개발자"
        val difficulty = when (profile.yearsOfExperience) {
            in 0..2 -> "주니어"
            in 3..5 -> "미드레벨"
            else -> "시니어"
        }
        
        return """
            당신은 기술 면접 전문가입니다.
            ${difficulty} ${targetPosition} 지원자를 위한 심화 기술 질문을 생성해주세요.
            
            지원자 기술 스택:
            ${skills.joinToString("\n") { "- ${it.name}: ${it.level} (${it.yearsOfExperience}년)" }}
            
            생성할 질문 카테고리:
            1. 알고리즘 & 자료구조 (2문제)
               - 난이도: ${difficulty}
               - 실무 연관성 높은 문제
            
            2. 시스템 디자인 (2문제)
               - 확장성, 성능, 신뢰성 고려
               - 실제 서비스 예시 활용
            
            3. 코드 리뷰 (1문제)
               - 버그 찾기 또는 개선점 제안
               - 실제 코드 스니펫 포함
            
            4. 기술 심화 (3문제)
               - 보유 기술 스택 관련
               - 내부 동작 원리
               - 트러블슈팅 경험
            
            각 질문에 포함할 내용:
            - 질문 내용
            - 난이도 (1-5)
            - 예상 소요시간
            - 평가 포인트
            - 모범 답안의 핵심 요소
            - 힌트 (선택적)
            
            JSON 형식으로 응답해주세요.
        """.trimIndent()
    }
    
    override fun getMaxTokens(): Int = 4000
    override fun getTemperature(): Double = 0.6
    
    override fun extractMetadata(llmResponse: LlmResponse): Map<String, Any> {
        return mapOf(
            "model" to llmResponse.model,
            "tokens_used" to llmResponse.usage.totalTokens,
            "agent_type" to specialization.name,
            "question_count" to 8
        )
    }
    
    override fun calculateConfidence(llmResponse: LlmResponse): Double {
        val hasJson = llmResponse.content.contains("questions") || 
                     llmResponse.content.contains("problems")
        return if (hasJson && llmResponse.content.length > 1000) 0.9 else 0.7
    }
    
    override fun getFallbackResponse(context: AgentContext): AgentResponse {
        val fallbackQuestions = """
            {
                "technical_questions": [
                    {
                        "category": "알고리즘",
                        "question": "배열에서 중복 없는 가장 긴 부분 문자열을 찾는 알고리즘을 설명하세요.",
                        "difficulty": 3,
                        "time_minutes": 30,
                        "evaluation_points": ["시간복잡도 이해", "슬라이딩 윈도우", "해시맵 활용"],
                        "key_concepts": ["Two Pointers", "Hash Map", "O(n) solution"],
                        "hint": "슬라이딩 윈도우와 해시맵을 활용해보세요"
                    },
                    {
                        "category": "시스템 디자인",
                        "question": "URL 단축 서비스를 설계하세요.",
                        "difficulty": 4,
                        "time_minutes": 45,
                        "evaluation_points": ["확장성", "데이터베이스 설계", "캐싱 전략", "분산 처리"],
                        "key_concepts": ["Base62 encoding", "NoSQL vs SQL", "Cache", "Load Balancer"],
                        "hint": "일일 10억 요청을 처리할 수 있어야 합니다"
                    }
                ]
            }
        """.trimIndent()
        
        return AgentResponse(
            agentName = name,
            content = fallbackQuestions,
            metadata = mapOf("fallback" to true),
            confidence = 0.5
        )
    }
}