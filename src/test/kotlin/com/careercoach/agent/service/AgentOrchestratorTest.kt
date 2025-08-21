package com.careercoach.agent.service

import com.careercoach.agent.base.*
import com.careercoach.agent.impl.*
import com.careercoach.llm.service.LlmService
import com.careercoach.domain.profile.repository.ProfileRepository
import com.careercoach.domain.profile.repository.TechnicalSkillRepository
import com.careercoach.domain.profile.entity.Profile
import com.careercoach.llm.dto.LlmResponse
import com.careercoach.llm.dto.TokenUsage
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.util.*
import org.junit.jupiter.api.Assertions.*

@SpringBootTest
class AgentOrchestratorTest {
    
    private lateinit var orchestrator: AgentOrchestrator
    private val llmService = mockk<LlmService>()
    private val profileRepository = mockk<ProfileRepository>()
    private val technicalSkillRepository = mockk<TechnicalSkillRepository>()
    
    private lateinit var interviewAgent: InterviewAgent
    private lateinit var technicalAgent: TechnicalAgent
    private lateinit var behavioralAgent: BehavioralAgent
    
    @BeforeEach
    fun setup() {
        clearAllMocks()
        
        interviewAgent = InterviewAgent(llmService, profileRepository, technicalSkillRepository)
        technicalAgent = TechnicalAgent(llmService, profileRepository, technicalSkillRepository)
        behavioralAgent = BehavioralAgent(llmService, profileRepository, technicalSkillRepository)
        
        orchestrator = AgentOrchestrator(listOf(interviewAgent, technicalAgent, behavioralAgent))
    }
    
    @Test
    fun `단일 에이전트 오케스트레이션 성공`() = runBlocking {
        // Given
        val profileId = 1L
        val context = AgentContext(
            profileId = profileId,
            targetPosition = "백엔드 개발자",
            targetCompany = "네이버",
            requestType = "interview"
        )
        
        val profile = mockk<Profile> {
            every { id } returns profileId
            every { name } returns "홍길동"
            every { yearsOfExperience } returns 5
            every { currentPosition } returns "개발자"
            every { experiences } returns mutableListOf()
            every { projects } returns mutableListOf()
        }
        
        every { profileRepository.findByIdWithExperiences(profileId) } returns Optional.of(profile)
        every { technicalSkillRepository.findByProfileId(profileId) } returns emptyList()
        every { 
            llmService.generateText(any(), any(), any())
        } returns LlmResponse(
            content = """
                {
                    "questions": [
                        {
                            "type": "경력 검증",
                            "question": "가장 자랑스러운 프로젝트를 설명해주세요.",
                            "intent": "프로젝트 경험과 기술 깊이 평가"
                        }
                    ]
                }
            """.trimIndent(),
            model = "gemini",
            usage = TokenUsage(100, 200, 300)
        )
        
        // When
        val response = orchestrator.orchestrate(context)
        
        // Then
        assertNotNull(response)
        assertTrue(response.success)
        assertEquals(1, response.responses.size)
        assertEquals("Interview Agent", response.responses[0].agentName)
        assertTrue(response.combinedContent.contains("Interview Agent"))
    }
    
    @Test
    fun `멀티 에이전트 병렬 실행 성공`() = runBlocking {
        // Given
        val profileId = 1L
        val contexts = listOf(
            AgentContext(profileId, "개발자", "카카오", "interview"),
            AgentContext(profileId, "개발자", "카카오", "technical"),
            AgentContext(profileId, "개발자", "카카오", "behavioral")
        )
        
        val profile = mockk<Profile> {
            every { id } returns profileId
            every { name } returns "테스트"
            every { yearsOfExperience } returns 3
            every { currentPosition } returns "주니어 개발자"
            every { experiences } returns mutableListOf()
            every { projects } returns mutableListOf()
        }
        
        every { profileRepository.findByIdWithExperiences(profileId) } returns Optional.of(profile)
        every { technicalSkillRepository.findByProfileId(profileId) } returns emptyList()
        
        every { 
            llmService.generateText(match { it.contains("면접관") }, any(), any())
        } returns LlmResponse(
            content = """{"questions": [{"question": "Interview question"}]}""",
            model = "gemini",
            usage = TokenUsage(100, 200, 300)
        )
        
        every { 
            llmService.generateText(match { it.contains("기술 면접") }, any(), any())
        } returns LlmResponse(
            content = """{"technical_questions": [{"question": "Technical question"}]}""",
            model = "gemini",
            usage = TokenUsage(100, 200, 300)
        )
        
        every { 
            llmService.generateText(match { it.contains("행동") }, any(), any())
        } returns LlmResponse(
            content = """{"behavioral_questions": [{"question": "Behavioral question"}]}""",
            model = "gemini",
            usage = TokenUsage(100, 200, 300)
        )
        
        // When
        val responses = orchestrator.executeParallel(contexts)
        
        // Then
        assertEquals(3, responses.size)
        responses.forEach { response ->
            assertTrue(response.success)
            assertTrue(response.responses.isNotEmpty())
        }
    }
    
    @Test
    fun `에이전트 실패 시 폴백 응답 반환`() = runBlocking {
        // Given
        val profileId = 1L
        val context = AgentContext(
            profileId = profileId,
            requestType = "technical"
        )
        
        val profile = mockk<Profile> {
            every { id } returns profileId
            every { name } returns "테스트"
            every { yearsOfExperience } returns 5
            every { currentPosition } returns "개발자"
            every { experiences } returns mutableListOf()
            every { projects } returns mutableListOf()
        }
        
        every { profileRepository.findByIdWithExperiences(profileId) } returns Optional.of(profile)
        every { technicalSkillRepository.findByProfileId(profileId) } returns emptyList()
        every { 
            llmService.generateText(any(), any(), any())
        } throws RuntimeException("LLM Service Error")
        
        // When
        val response = orchestrator.orchestrate(context)
        
        // Then
        assertTrue(response.success)
        assertEquals(1, response.responses.size)
        assertTrue(response.responses[0].content.contains("technical_questions"))
        assertTrue(response.responses[0].confidence < 0.6) // 폴백 응답은 낮은 신뢰도
    }
    
    @Test
    fun `캐시 기능 테스트`() = runBlocking {
        // Given
        val profileId = 1L
        val context = AgentContext(
            profileId = profileId,
            requestType = "interview"
        )
        
        val profile = mockk<Profile> {
            every { id } returns profileId
            every { name } returns "캐시테스트"
            every { yearsOfExperience } returns 7
            every { currentPosition } returns "시니어 개발자"
            every { experiences } returns mutableListOf()
            every { projects } returns mutableListOf()
        }
        
        every { profileRepository.findByIdWithExperiences(profileId) } returns Optional.of(profile)
        every { technicalSkillRepository.findByProfileId(profileId) } returns emptyList()
        every { 
            llmService.generateText(any(), any(), any())
        } returns LlmResponse(
            content = """{"questions": [{"question": "Cached question"}]}""",
            model = "gemini",
            usage = TokenUsage(100, 200, 300)
        )
        
        // When - 첫 번째 호출
        val response1 = orchestrator.orchestrate(context)
        
        // When - 두 번째 호출 (캐시된 응답 사용)
        val response2 = orchestrator.orchestrate(context)
        
        // Then
        assertEquals(response1.responses[0].content, response2.responses[0].content)
        
        // LLM 서비스는 한 번만 호출되어야 함
        verify(exactly = 1) { llmService.generateText(any(), any(), any()) }
        
        // 캐시 클리어 후
        orchestrator.clearCache()
        
        // When - 세 번째 호출 (캐시 클리어 후)
        val response3 = orchestrator.orchestrate(context)
        
        // Then - LLM 서비스가 다시 호출됨
        verify(exactly = 2) { llmService.generateText(any(), any(), any()) }
    }
    
    @Test
    fun `사용 가능한 에이전트 목록 조회`() {
        // When
        val agents = orchestrator.getAvailableAgents()
        
        // Then
        assertEquals(3, agents.size)
        assertTrue(agents.any { it.name == "Interview Agent" })
        assertTrue(agents.any { it.name == "Technical Agent" })
        assertTrue(agents.any { it.name == "Behavioral Agent" })
    }
}