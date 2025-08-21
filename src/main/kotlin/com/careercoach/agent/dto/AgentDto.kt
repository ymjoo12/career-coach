package com.careercoach.agent.dto

import com.careercoach.agent.base.AgentSpecialization
import com.careercoach.agent.service.OrchestratedResponse
import com.careercoach.agent.service.AgentInfo
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

@Schema(description = "에이전트 요청")
data class AgentRequest(
    @field:NotNull
    @field:Positive
    @Schema(description = "프로필 ID", example = "1")
    val profileId: Long,
    
    @Schema(description = "목표 포지션", example = "시니어 백엔드 개발자")
    val targetPosition: String? = null,
    
    @Schema(description = "목표 회사", example = "네이버")
    val targetCompany: String? = null,
    
    @field:NotNull
    @Schema(description = "요청 타입", example = "interview")
    val requestType: String,
    
    @Schema(description = "추가 파라미터")
    val parameters: Map<String, Any> = emptyMap()
)

@Schema(description = "종합 면접 질문 요청")
data class ComprehensiveInterviewRequest(
    @field:NotNull
    @field:Positive
    @Schema(description = "프로필 ID", example = "1")
    val profileId: Long,
    
    @Schema(description = "목표 포지션", example = "시니어 백엔드 개발자")
    val targetPosition: String? = null,
    
    @Schema(description = "목표 회사", example = "네이버")
    val targetCompany: String? = null,
    
    @Schema(description = "난이도", example = "INTERMEDIATE")
    val difficulty: String = "INTERMEDIATE",
    
    @Schema(description = "시니어리티 레벨", example = "SENIOR")
    val seniorityLevel: String = "MID"
)

@Schema(description = "에이전트 오케스트레이션 응답")
data class AgentOrchestratedResponse(
    @Schema(description = "요청 컨텍스트")
    val context: Map<String, Any>,
    
    @Schema(description = "각 에이전트 응답")
    val agentResponses: List<AgentResponseDto>,
    
    @Schema(description = "통합된 응답 내용")
    val combinedContent: String,
    
    @Schema(description = "성공 여부")
    val success: Boolean,
    
    @Schema(description = "메타데이터")
    val metadata: Map<String, Any>
) {
    companion object {
        fun from(response: OrchestratedResponse): AgentOrchestratedResponse {
            return AgentOrchestratedResponse(
                context = mapOf(
                    "profileId" to response.context.profileId,
                    "requestType" to response.context.requestType,
                    "targetPosition" to (response.context.targetPosition ?: ""),
                    "targetCompany" to (response.context.targetCompany ?: "")
                ),
                agentResponses = response.responses.map { agentResponse ->
                    AgentResponseDto(
                        agentName = agentResponse.agentName,
                        content = agentResponse.content,
                        confidence = agentResponse.confidence,
                        metadata = agentResponse.metadata
                    )
                },
                combinedContent = response.combinedContent,
                success = response.success,
                metadata = response.metadata
            )
        }
    }
}

@Schema(description = "개별 에이전트 응답")
data class AgentResponseDto(
    @Schema(description = "에이전트 이름")
    val agentName: String,
    
    @Schema(description = "응답 내용")
    val content: String,
    
    @Schema(description = "신뢰도 (0.0 ~ 1.0)")
    val confidence: Double,
    
    @Schema(description = "메타데이터")
    val metadata: Map<String, Any>
)

@Schema(description = "종합 면접 질문 응답")
data class ComprehensiveInterviewResponse(
    @Schema(description = "프로필 ID")
    val profileId: Long,
    
    @Schema(description = "목표 포지션")
    val targetPosition: String?,
    
    @Schema(description = "목표 회사")
    val targetCompany: String?,
    
    @Schema(description = "질문 섹션들")
    val sections: List<InterviewSection>,
    
    @Schema(description = "총 질문 수")
    val totalQuestionCount: Int,
    
    @Schema(description = "평균 신뢰도")
    val averageConfidence: Double
)

@Schema(description = "면접 질문 섹션")
data class InterviewSection(
    @Schema(description = "에이전트 이름")
    val agentName: String,
    
    @Schema(description = "질문 내용")
    val content: String,
    
    @Schema(description = "신뢰도")
    val confidence: Double,
    
    @Schema(description = "메타데이터")
    val metadata: Map<String, Any>
)

@Schema(description = "에이전트 정보 응답")
data class AgentInfoResponse(
    @Schema(description = "에이전트 이름")
    val name: String,
    
    @Schema(description = "에이전트 설명")
    val description: String,
    
    @Schema(description = "전문 분야")
    val specialization: String
) {
    companion object {
        fun from(info: AgentInfo): AgentInfoResponse {
            return AgentInfoResponse(
                name = info.name,
                description = info.description,
                specialization = info.specialization.name
            )
        }
    }
}