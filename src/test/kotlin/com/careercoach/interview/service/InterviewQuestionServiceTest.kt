package com.careercoach.interview.service

import com.careercoach.common.exception.ResourceNotFoundException
import com.careercoach.domain.profile.entity.Profile
import com.careercoach.domain.profile.entity.TechnicalSkill
import com.careercoach.domain.profile.repository.ProfileRepository
import com.careercoach.domain.profile.repository.TechnicalSkillRepository
import com.careercoach.interview.dto.*
import com.careercoach.llm.dto.LlmResponse
import com.careercoach.llm.dto.TokenUsage
import com.careercoach.llm.service.LlmService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
class InterviewQuestionServiceTest {
    
    private lateinit var service: InterviewQuestionService
    private val profileRepository = mockk<ProfileRepository>()
    private val technicalSkillRepository = mockk<TechnicalSkillRepository>()
    private val llmService = mockk<LlmService>()
    private val objectMapper = ObjectMapper()
    
    @BeforeEach
    fun setup() {
        clearAllMocks()
        service = InterviewQuestionService(
            profileRepository,
            technicalSkillRepository,
            llmService,
            objectMapper
        )
    }
    
    @Test
    fun `면접 질문 생성 성공`() {
        // Given
        val profileId = 1L
        val request = InterviewQuestionRequest(
            profileId = profileId,
            targetPosition = "백엔드 개발자",
            targetCompany = "네이버",
            questionCount = 3,
            difficulty = QuestionDifficulty.MEDIUM
        )
        
        val profile = mockk<Profile> {
            every { name } returns "홍길동"
            every { yearsOfExperience } returns 5
            every { currentPosition } returns "주니어 개발자"
            every { experiences } returns emptyList()
            every { projects } returns emptyList()
        }
        
        val skills = listOf(
            mockk<TechnicalSkill> {
                every { category } returns "Backend"
                every { name } returns "Spring Boot"
                every { level } returns "ADVANCED"
            }
        )
        
        val llmResponseContent = """
            {
                "questions": [
                    {
                        "id": 1,
                        "category": "Technical",
                        "type": "TECHNICAL",
                        "question": "Spring Boot의 자동 설정 원리를 설명해주세요.",
                        "difficulty": "MEDIUM",
                        "expectedAnswer": "AutoConfiguration과 @Conditional 어노테이션 기반 설명",
                        "evaluationCriteria": ["기술 이해도", "설명 능력"],
                        "followUpQuestions": ["실제 프로젝트 적용 사례는?"]
                    },
                    {
                        "id": 2,
                        "category": "System Design",
                        "type": "SYSTEM_DESIGN",
                        "question": "대용량 트래픽 처리를 위한 시스템 설계 방안을 제시해주세요.",
                        "difficulty": "HARD",
                        "expectedAnswer": "로드밸런싱, 캐싱, 데이터베이스 샤딩 등",
                        "evaluationCriteria": ["시스템 설계 능력", "확장성 고려"],
                        "followUpQuestions": ["캐싱 전략은?"]
                    }
                ]
            }
        """.trimIndent()
        
        val enhancedResponseContent = """
            {
                "enhancements": [
                    {
                        "id": 1,
                        "enhanced_question": "네이버 서비스에서 Spring Boot 자동 설정을 어떻게 활용하시겠습니까?",
                        "context": "네이버의 MSA 환경 고려",
                        "tags": ["Spring", "MSA", "네이버"]
                    }
                ]
            }
        """.trimIndent()
        
        every { profileRepository.findByIdWithExperiences(profileId) } returns Optional.of(profile)
        every { technicalSkillRepository.findByProfileId(profileId) } returns skills
        every { 
            llmService.generateFromTemplate(
                templateName = "interview_question_generation",
                params = any(),
                maxTokens = 4000,
                temperature = 0.8
            )
        } returns LlmResponse(
            content = llmResponseContent,
            model = "gemini",
            usage = TokenUsage(100, 200, 300)
        )
        every {
            llmService.generateText(
                prompt = any(),
                maxTokens = 3000,
                temperature = 0.7
            )
        } returns LlmResponse(
            content = enhancedResponseContent,
            model = "gemini",
            usage = TokenUsage(100, 200, 300)
        )
        
        // When
        val response = service.generateInterviewQuestions(request)
        
        // Then
        assertNotNull(response)
        assertEquals(profileId, response.profileId)
        assertEquals("백엔드 개발자", response.targetPosition)
        assertEquals("네이버", response.targetCompany)
        assertTrue(response.questions.isNotEmpty())
        
        val firstQuestion = response.questions[0]
        assertTrue(firstQuestion.question.contains("네이버"))
        assertEquals("네이버의 MSA 환경 고려", firstQuestion.context)
        
        verify(exactly = 1) { profileRepository.findByIdWithExperiences(profileId) }
        verify(exactly = 1) { technicalSkillRepository.findByProfileId(profileId) }
    }
    
    @Test
    fun `프로필이 없을 때 예외 발생`() {
        // Given
        val profileId = 999L
        val request = InterviewQuestionRequest(
            profileId = profileId,
            targetPosition = "개발자"
        )
        
        every { profileRepository.findByIdWithExperiences(profileId) } returns Optional.empty()
        
        // When & Then
        assertThrows<ResourceNotFoundException> {
            service.generateInterviewQuestions(request)
        }
    }
    
    @Test
    fun `후속 질문 생성 성공`() {
        // Given
        val request = FollowUpQuestionRequest(
            profileId = 1L,
            originalQuestion = "Spring Boot의 장점은?",
            userAnswer = "자동 설정과 빠른 개발이 가능합니다.",
            questionCount = 2
        )
        
        val llmResponseContent = """
            {
                "follow_ups": [
                    {
                        "question": "자동 설정의 구체적인 동작 원리를 설명해주세요.",
                        "purpose": "기술적 깊이 확인",
                        "expected_depth": "AutoConfiguration 메커니즘 이해"
                    },
                    {
                        "question": "실제 프로젝트에서 자동 설정을 커스터마이징한 경험이 있나요?",
                        "purpose": "실무 경험 확인",
                        "expected_depth": "구체적인 사례와 해결 과정"
                    }
                ]
            }
        """.trimIndent()
        
        every {
            llmService.generateChatResponse(
                messages = any(),
                maxTokens = 1500,
                temperature = 0.8
            )
        } returns LlmResponse(
            content = llmResponseContent,
            model = "gemini",
            usage = TokenUsage(100, 200, 300)
        )
        
        // When
        val response = service.generateFollowUpQuestions(request)
        
        // Then
        assertNotNull(response)
        assertEquals("Spring Boot의 장점은?", response.originalQuestion)
        assertEquals(2, response.followUpQuestions.size)
        
        val firstFollowUp = response.followUpQuestions[0]
        assertTrue(firstFollowUp.question.contains("자동 설정"))
        assertEquals("기술적 깊이 확인", firstFollowUp.purpose)
    }
    
    @Test
    fun `LLM 실패 시 폴백 질문 반환`() {
        // Given
        val profileId = 1L
        val request = InterviewQuestionRequest(
            profileId = profileId,
            targetPosition = "개발자"
        )
        
        val profile = mockk<Profile> {
            every { name } returns "테스트"
            every { yearsOfExperience } returns 3
            every { currentPosition } returns "개발자"
            every { experiences } returns emptyList()
            every { projects } returns emptyList()
        }
        
        every { profileRepository.findByIdWithExperiences(profileId) } returns Optional.of(profile)
        every { technicalSkillRepository.findByProfileId(profileId) } returns emptyList()
        every { 
            llmService.generateFromTemplate(any(), any(), any(), any())
        } throws RuntimeException("LLM Service Error")
        
        // When
        val response = service.generateInterviewQuestions(request)
        
        // Then
        assertNotNull(response)
        assertTrue(response.questions.isNotEmpty())
        assertEquals(2, response.questions.size) // 폴백 질문 2개
        assertTrue(response.questions[0].question.contains("기술 스택"))
    }
}