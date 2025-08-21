package com.careercoach.learning.service

import com.careercoach.common.exception.ResourceNotFoundException
import com.careercoach.domain.profile.entity.Profile
import com.careercoach.domain.profile.entity.TechnicalSkill
import com.careercoach.domain.profile.repository.ProfileRepository
import com.careercoach.domain.profile.repository.TechnicalSkillRepository
import com.careercoach.learning.dto.*
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
class LearningPathServiceTest {
    
    private lateinit var service: LearningPathService
    private val profileRepository = mockk<ProfileRepository>()
    private val technicalSkillRepository = mockk<TechnicalSkillRepository>()
    private val llmService = mockk<LlmService>()
    private val objectMapper = ObjectMapper()
    
    @BeforeEach
    fun setup() {
        clearAllMocks()
        service = LearningPathService(
            profileRepository,
            technicalSkillRepository,
            llmService,
            objectMapper
        )
    }
    
    @Test
    fun `학습 경로 생성 성공`() {
        // Given
        val profileId = 1L
        val request = LearningPathRequest(
            profileId = profileId,
            targetPosition = "시니어 백엔드 개발자",
            targetCompany = "네이버",
            dailyStudyHours = 3,
            deadlineMonths = 6,
            budgetMonthly = 100000
        )
        
        val profile = mockk<Profile> {
            every { name } returns "홍길동"
            every { yearsOfExperience } returns 3
            every { currentPosition } returns "주니어 백엔드 개발자"
            every { experiences } returns emptyList()
            every { projects } returns emptyList()
        }
        
        val skills = listOf(
            mockk<TechnicalSkill> {
                every { category } returns "Backend"
                every { name } returns "Spring Boot"
                every { level } returns "INTERMEDIATE"
                every { yearsOfExperience } returns 2
            }
        )
        
        val skillGapResponseContent = """
            {
                "current_skills": [
                    {
                        "skill": "Spring Boot",
                        "current_level": "INTERMEDIATE",
                        "years_of_experience": 2.0
                    }
                ],
                "required_skills": [
                    {
                        "skill": "Spring Boot",
                        "required_level": "EXPERT",
                        "importance": "CRITICAL",
                        "market_demand": "HIGH"
                    },
                    {
                        "skill": "Kubernetes",
                        "required_level": "ADVANCED",
                        "importance": "HIGH",
                        "market_demand": "VERY_HIGH"
                    }
                ],
                "gap_skills": [
                    {
                        "skill": "Kubernetes",
                        "current_level": null,
                        "required_level": "ADVANCED",
                        "gap_size": "LARGE",
                        "estimated_time_to_learn": 12,
                        "priority": "HIGH"
                    }
                ],
                "transferable_skills": ["Problem Solving"],
                "priority_areas": ["Cloud Native", "System Design"]
            }
        """.trimIndent()
        
        val roadmapResponseContent = """
            {
                "roadmap": {
                    "totalDuration": "6개월",
                    "phases": [
                        {
                            "phase": 1,
                            "title": "기초 강화",
                            "description": "Spring Boot 심화 학습",
                            "duration": "8주",
                            "skills": ["Spring Boot Advanced", "JPA"],
                            "resources": [
                                {
                                    "type": "COURSE",
                                    "title": "Spring Boot 마스터",
                                    "url": "https://example.com",
                                    "platform": "Udemy",
                                    "estimatedTime": "40시간",
                                    "cost": "50000원"
                                }
                            ],
                            "projects": ["REST API 프로젝트"],
                            "milestones": ["API 설계 완료", "테스트 작성"]
                        },
                        {
                            "phase": 2,
                            "title": "클라우드 네이티브",
                            "description": "Kubernetes 학습",
                            "duration": "16주",
                            "skills": ["Docker", "Kubernetes"],
                            "resources": [],
                            "projects": ["마이크로서비스 배포"],
                            "milestones": ["K8s 클러스터 구축"]
                        }
                    ],
                    "weeklySchedule": {
                        "monday": ["강의 시청 2시간"],
                        "tuesday": ["실습 2시간"],
                        "wednesday": ["강의 시청 2시간"],
                        "thursday": ["실습 2시간"],
                        "friday": ["복습 2시간"],
                        "saturday": ["프로젝트 4시간"],
                        "sunday": ["휴식"]
                    }
                }
            }
        """.trimIndent()
        
        every { profileRepository.findByIdWithExperiences(profileId) } returns Optional.of(profile)
        every { technicalSkillRepository.findByProfileId(profileId) } returns skills
        every { 
            llmService.generateText(
                prompt = any(),
                maxTokens = 2000,
                temperature = 0.7
            )
        } returns LlmResponse(
            content = skillGapResponseContent,
            model = "gemini",
            usage = TokenUsage(100, 200, 300)
        )
        every {
            llmService.generateFromTemplate(
                templateName = "learning_path_generation",
                params = any(),
                maxTokens = 4000,
                temperature = 0.8
            )
        } returns LlmResponse(
            content = roadmapResponseContent,
            model = "gemini",
            usage = TokenUsage(100, 200, 300)
        )
        
        // When
        val response = service.generateLearningPath(request)
        
        // Then
        assertNotNull(response)
        assertEquals(profileId, response.profileId)
        assertEquals("시니어 백엔드 개발자", response.targetPosition)
        assertEquals("네이버", response.targetCompany)
        assertTrue(response.roadmap.phases.isNotEmpty())
        assertEquals(2, response.roadmap.phases.size)
        
        val firstPhase = response.roadmap.phases[0]
        assertEquals("기초 강화", firstPhase.title)
        assertTrue(firstPhase.resources.isNotEmpty())
        
        verify(exactly = 1) { profileRepository.findByIdWithExperiences(profileId) }
        verify(exactly = 1) { technicalSkillRepository.findByProfileId(profileId) }
    }
    
    @Test
    fun `프로필이 없을 때 예외 발생`() {
        // Given
        val profileId = 999L
        val request = LearningPathRequest(
            profileId = profileId,
            targetPosition = "개발자"
        )
        
        every { profileRepository.findByIdWithExperiences(profileId) } returns Optional.empty()
        
        // When & Then
        assertThrows<ResourceNotFoundException> {
            service.generateLearningPath(request)
        }
    }
    
    @Test
    fun `스킬 갭 분석 성공`() {
        // Given
        val profileId = 1L
        val request = SkillGapAnalysisRequest(
            profileId = profileId,
            targetPosition = "시니어 개발자",
            targetCompany = "카카오"
        )
        
        val profile = mockk<Profile> {
            every { name } returns "테스트"
            every { yearsOfExperience } returns 5
            every { currentPosition } returns "백엔드 개발자"
            every { experiences } returns emptyList()
            every { projects } returns emptyList()
        }
        
        val skills = listOf(
            mockk<TechnicalSkill> {
                every { category } returns "Backend"
                every { name } returns "Java"
                every { level } returns "ADVANCED"
                every { yearsOfExperience } returns 5
            }
        )
        
        val skillGapResponseContent = """
            {
                "current_skills": [
                    {
                        "skill": "Java",
                        "current_level": "ADVANCED",
                        "years_of_experience": 5.0
                    }
                ],
                "required_skills": [
                    {
                        "skill": "Kotlin",
                        "required_level": "ADVANCED",
                        "importance": "HIGH",
                        "market_demand": "HIGH"
                    }
                ],
                "gap_skills": [
                    {
                        "skill": "Kotlin",
                        "current_level": null,
                        "required_level": "ADVANCED",
                        "gap_size": "MEDIUM",
                        "estimated_time_to_learn": 8,
                        "priority": "HIGH"
                    }
                ],
                "transferable_skills": ["OOP", "Spring"],
                "priority_areas": ["Kotlin", "Coroutines"]
            }
        """.trimIndent()
        
        every { profileRepository.findByIdWithExperiences(profileId) } returns Optional.of(profile)
        every { technicalSkillRepository.findByProfileId(profileId) } returns skills
        every { 
            llmService.generateText(any(), any(), any())
        } returns LlmResponse(
            content = skillGapResponseContent,
            model = "gemini",
            usage = TokenUsage(100, 200, 300)
        )
        
        // When
        val response = service.analyzeSkillGap(request)
        
        // Then
        assertNotNull(response)
        assertTrue(response.currentSkills.isNotEmpty())
        assertTrue(response.requiredSkills.isNotEmpty())
        assertTrue(response.gapSkills.isNotEmpty())
        assertEquals("Kotlin", response.gapSkills[0].skill)
        assertTrue(response.transferableSkills.contains("OOP"))
    }
    
    @Test
    fun `LLM 실패 시 폴백 학습 경로 반환`() {
        // Given
        val profileId = 1L
        val request = LearningPathRequest(
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
            llmService.generateText(any(), any(), any())
        } throws RuntimeException("LLM Service Error")
        every {
            llmService.generateFromTemplate(any(), any(), any(), any())
        } throws RuntimeException("LLM Service Error")
        
        // When
        val response = service.generateLearningPath(request)
        
        // Then
        assertNotNull(response)
        assertTrue(response.roadmap.phases.isNotEmpty())
        assertEquals(2, response.roadmap.phases.size) // 폴백 로드맵은 2개 페이즈
        assertEquals("Foundation Building", response.roadmap.phases[0].title)
    }
}