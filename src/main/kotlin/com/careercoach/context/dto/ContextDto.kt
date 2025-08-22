package com.careercoach.context.dto

import com.careercoach.context.model.*
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

// Request DTOs

@Schema(description = "세션 생성 요청")
data class CreateSessionRequest(
    @field:NotNull
    @field:Positive
    @Schema(description = "프로필 ID", example = "1")
    val profileId: Long,
    
    @Schema(description = "초기 의도", example = "INTERVIEW_PREPARATION")
    val initialIntent: String? = null
)

@Schema(description = "상호작용 요청")
data class InteractionRequest(
    @field:NotNull
    @field:Size(min = 1, max = 500)
    @Schema(description = "액션", example = "generate_interview_questions")
    val action: String,
    
    @Schema(description = "결과", example = "Questions generated successfully")
    val result: String? = null,
    
    @Schema(description = "의도", example = "INTERVIEW_PREPARATION")
    val intent: String? = null,
    
    @Schema(description = "피드백")
    val feedback: FeedbackDto? = null
)

@Schema(description = "피드백 정보")
data class FeedbackDto(
    @Schema(description = "평점 (1-5)", example = "4")
    val rating: Int,
    
    @Schema(description = "도움 여부", example = "true")
    val helpful: Boolean,
    
    @Schema(description = "코멘트")
    val comment: String? = null
)

@Schema(description = "의도 분석 요청")
data class IntentAnalysisRequest(
    @field:NotNull
    @field:Size(min = 1, max = 1000)
    @Schema(description = "분석할 쿼리", example = "네이버 면접 준비를 도와주세요")
    val query: String
)

@Schema(description = "응답 조정 요청")
data class AdjustResponseRequest(
    @field:NotNull
    @Schema(description = "기본 응답")
    val baseResponse: String,
    
    @Schema(description = "메타데이터")
    val metadata: Map<String, Any> = emptyMap()
)

// Response DTOs

@Schema(description = "세션 응답")
data class SessionResponse(
    @Schema(description = "세션 ID")
    val sessionId: String,
    
    @Schema(description = "프로필 ID")
    val profileId: Long,
    
    @Schema(description = "현재 의도")
    val currentIntent: String,
    
    @Schema(description = "현재 단계")
    val currentPhase: String,
    
    @Schema(description = "참여도 레벨")
    val engagementLevel: String,
    
    @Schema(description = "선호도 설정")
    val preferences: PreferencesDto
)

@Schema(description = "세션 상세 응답")
data class SessionDetailResponse(
    @Schema(description = "세션 ID")
    val sessionId: String,
    
    @Schema(description = "프로필 ID")
    val profileId: Long,
    
    @Schema(description = "현재 의도")
    val currentIntent: String,
    
    @Schema(description = "현재 단계")
    val currentPhase: String,
    
    @Schema(description = "참여도 레벨")
    val engagementLevel: String,
    
    @Schema(description = "세션 지속 시간 (ms)")
    val sessionDuration: Long,
    
    @Schema(description = "질문 수")
    val questionsAsked: Int,
    
    @Schema(description = "완료된 작업 수")
    val tasksCompleted: Int,
    
    @Schema(description = "현재 주제")
    val currentTopic: String?,
    
    @Schema(description = "최근 상호작용")
    val recentInteractions: List<InteractionDto>,
    
    @Schema(description = "선호도 설정")
    val preferences: PreferencesDto
)

@Schema(description = "상호작용 응답")
data class InteractionResponse(
    @Schema(description = "세션 ID")
    val sessionId: String,
    
    @Schema(description = "업데이트된 의도")
    val updatedIntent: String,
    
    @Schema(description = "업데이트된 단계")
    val updatedPhase: String,
    
    @Schema(description = "참여도 레벨")
    val engagementLevel: String,
    
    @Schema(description = "신뢰도")
    val confidence: Double,
    
    @Schema(description = "메시지")
    val message: String
)

@Schema(description = "의도 분석 응답")
data class IntentAnalysisResponse(
    @Schema(description = "식별된 의도")
    val intent: String,
    
    @Schema(description = "신뢰도")
    val confidence: Double,
    
    @Schema(description = "키워드")
    val keywords: List<String>,
    
    @Schema(description = "제안 액션")
    val suggestedActions: List<String>,
    
    @Schema(description = "관련 의도")
    val relatedIntents: List<String>
)

@Schema(description = "조정된 응답")
data class AdjustedResponseDto(
    @Schema(description = "원본 콘텐츠")
    val originalContent: String,
    
    @Schema(description = "조정된 콘텐츠")
    val adjustedContent: String,
    
    @Schema(description = "적용된 조정 사항")
    val adjustments: List<String>,
    
    @Schema(description = "응답 스타일")
    val style: ResponseStyleDto?,
    
    @Schema(description = "메타데이터")
    val metadata: Map<String, Any>
)

@Schema(description = "추천 응답")
data class RecommendationsResponse(
    @Schema(description = "콘텐츠 추천")
    val content: List<ContentRecommendationDto>,
    
    @Schema(description = "액션 추천")
    val actions: List<ActionRecommendationDto>,
    
    @Schema(description = "학습 추천")
    val learning: List<LearningRecommendationDto>,
    
    @Schema(description = "우선순위")
    val priority: String,
    
    @Schema(description = "추천 이유")
    val reasoning: String
)

// Supporting DTOs

@Schema(description = "선호도 설정")
data class PreferencesDto(
    @Schema(description = "언어")
    val language: String,
    
    @Schema(description = "난이도")
    val difficulty: String,
    
    @Schema(description = "학습 스타일")
    val learningStyle: String,
    
    @Schema(description = "커뮤니케이션 스타일")
    val communicationStyle: String,
    
    @Schema(description = "관심 분야")
    val focusAreas: List<String>,
    
    @Schema(description = "선호 회사")
    val preferredCompanies: List<String>,
    
    @Schema(description = "선호 포지션")
    val preferredPositions: List<String>
) {
    companion object {
        fun from(preferences: UserPreferences): PreferencesDto {
            return PreferencesDto(
                language = preferences.language.name,
                difficulty = preferences.difficulty.name,
                learningStyle = preferences.learningStyle.name,
                communicationStyle = preferences.communicationStyle.name,
                focusAreas = preferences.focusAreas,
                preferredCompanies = preferences.preferredCompanies,
                preferredPositions = preferences.preferredPositions
            )
        }
    }
}

@Schema(description = "상호작용 정보")
data class InteractionDto(
    @Schema(description = "타임스탬프")
    val timestamp: String,
    
    @Schema(description = "의도")
    val intent: String,
    
    @Schema(description = "액션")
    val action: String,
    
    @Schema(description = "결과")
    val result: String?,
    
    @Schema(description = "신뢰도")
    val confidence: Double
)

@Schema(description = "응답 스타일")
data class ResponseStyleDto(
    @Schema(description = "톤")
    val tone: String,
    
    @Schema(description = "길이")
    val length: String,
    
    @Schema(description = "복잡도")
    val complexity: String,
    
    @Schema(description = "예시 포함 여부")
    val includeExamples: Boolean,
    
    @Schema(description = "언어")
    val language: String
)

@Schema(description = "콘텐츠 추천")
data class ContentRecommendationDto(
    @Schema(description = "타입")
    val type: String,
    
    @Schema(description = "제목")
    val title: String,
    
    @Schema(description = "설명")
    val description: String,
    
    @Schema(description = "우선순위")
    val priority: Int
)

@Schema(description = "액션 추천")
data class ActionRecommendationDto(
    @Schema(description = "액션")
    val action: String,
    
    @Schema(description = "이유")
    val reason: String,
    
    @Schema(description = "긴급도")
    val urgency: String
)

@Schema(description = "학습 추천")
data class LearningRecommendationDto(
    @Schema(description = "스킬")
    val skill: String,
    
    @Schema(description = "현재 레벨")
    val currentLevel: String,
    
    @Schema(description = "목표 레벨")
    val targetLevel: String,
    
    @Schema(description = "리소스")
    val resources: List<String>
)

@Schema(description = "세션 요약")
data class SessionSummaryDto(
    @Schema(description = "세션 ID")
    val sessionId: String,
    
    @Schema(description = "현재 의도")
    val currentIntent: String,
    
    @Schema(description = "현재 단계")
    val currentPhase: String,
    
    @Schema(description = "참여도 레벨")
    val engagementLevel: String,
    
    @Schema(description = "마지막 활동")
    val lastActivity: String,
    
    @Schema(description = "세션 지속 시간 (ms)")
    val sessionDuration: Long,
    
    @Schema(description = "상호작용 수")
    val interactionCount: Int
)

@Schema(description = "컨텍스트 통계")
data class ContextStatisticsDto(
    @Schema(description = "활성 세션 수")
    val activeSessions: Int,
    
    @Schema(description = "고유 프로필 수")
    val uniqueProfiles: Int,
    
    @Schema(description = "의도 분포")
    val intentDistribution: Map<String, Int>,
    
    @Schema(description = "단계 분포")
    val phaseDistribution: Map<String, Int>,
    
    @Schema(description = "참여도 분포")
    val engagementDistribution: Map<String, Int>,
    
    @Schema(description = "평균 세션 지속 시간")
    val averageSessionDuration: Double
)