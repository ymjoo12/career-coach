package com.careercoach.agent.controller

import com.careercoach.agent.base.AgentContext
import com.careercoach.agent.service.AgentOrchestrator
import com.careercoach.agent.dto.*
import com.careercoach.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/agents")
@Tag(name = "Multi-Agent System", description = "Multi-Agent 시스템 API")
class AgentController(
    private val agentOrchestrator: AgentOrchestrator
) {
    
    @PostMapping("/orchestrate")
    @Operation(summary = "Multi-Agent 오케스트레이션 실행", description = "여러 에이전트를 조율하여 요청 처리")
    fun orchestrate(@Valid @RequestBody request: AgentRequest): ApiResponse<AgentOrchestratedResponse> {
        val context = AgentContext(
            profileId = request.profileId,
            targetPosition = request.targetPosition,
            targetCompany = request.targetCompany,
            requestType = request.requestType,
            parameters = request.parameters
        )
        
        val response = runBlocking {
            agentOrchestrator.orchestrate(context)
        }
        
        return ApiResponse.success(
            AgentOrchestratedResponse.from(response)
        )
    }
    
    @PostMapping("/interview-questions")
    @Operation(summary = "종합 면접 질문 생성", description = "Interview, Technical, Behavioral 에이전트를 활용한 종합 질문 생성")
    fun generateComprehensiveQuestions(
        @Valid @RequestBody request: ComprehensiveInterviewRequest
    ): ApiResponse<ComprehensiveInterviewResponse> {
        val contexts = listOf(
            AgentContext(
                profileId = request.profileId,
                targetPosition = request.targetPosition,
                targetCompany = request.targetCompany,
                requestType = "interview",
                parameters = mapOf("focus" to "general")
            ),
            AgentContext(
                profileId = request.profileId,
                targetPosition = request.targetPosition,
                targetCompany = request.targetCompany,
                requestType = "technical",
                parameters = mapOf("difficulty" to request.difficulty)
            ),
            AgentContext(
                profileId = request.profileId,
                targetPosition = request.targetPosition,
                targetCompany = request.targetCompany,
                requestType = "behavioral",
                parameters = mapOf("seniority" to request.seniorityLevel)
            )
        )
        
        val responses = agentOrchestrator.executeParallel(contexts)
        
        return ApiResponse.success(
            ComprehensiveInterviewResponse(
                profileId = request.profileId,
                targetPosition = request.targetPosition,
                targetCompany = request.targetCompany,
                sections = responses.map { orchestrated ->
                    InterviewSection(
                        agentName = orchestrated.responses.firstOrNull()?.agentName ?: "Unknown",
                        content = orchestrated.combinedContent,
                        confidence = orchestrated.responses.firstOrNull()?.confidence ?: 0.0,
                        metadata = orchestrated.metadata
                    )
                },
                totalQuestionCount = responses.size * 5,
                averageConfidence = responses.flatMap { it.responses }
                    .map { it.confidence }.average()
            )
        )
    }
    
    @GetMapping("/available")
    @Operation(summary = "사용 가능한 에이전트 목록 조회", description = "시스템에 등록된 모든 에이전트 정보 조회")
    fun getAvailableAgents(): ApiResponse<List<AgentInfoResponse>> {
        val agents = agentOrchestrator.getAvailableAgents()
        return ApiResponse.success(
            agents.map { AgentInfoResponse.from(it) }
        )
    }
    
    @DeleteMapping("/cache")
    @Operation(summary = "에이전트 캐시 초기화", description = "모든 에이전트 응답 캐시 삭제")
    fun clearCache(): ApiResponse<String> {
        agentOrchestrator.clearCache()
        return ApiResponse.success("Cache cleared successfully")
    }
}