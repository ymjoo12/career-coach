package com.careercoach.context.controller

import com.careercoach.common.dto.ApiResponse
import com.careercoach.context.dto.*
import com.careercoach.context.model.UserIntent
import com.careercoach.context.model.UserFeedback
import com.careercoach.context.service.ContextManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/context")
@Tag(name = "Context Intelligence", description = "컨텍스트 인텔리전스 API")
class ContextController(
    private val contextManager: ContextManager
) {
    
    @PostMapping("/sessions")
    @Operation(summary = "컨텍스트 세션 생성", description = "새로운 사용자 컨텍스트 세션 생성")
    fun createSession(@Valid @RequestBody request: CreateSessionRequest): ApiResponse<SessionResponse> {
        val context = contextManager.createContext(
            profileId = request.profileId,
            initialIntent = request.initialIntent?.let { UserIntent.valueOf(it) }
        )
        
        return ApiResponse.success(
            SessionResponse(
                sessionId = context.sessionId,
                profileId = context.profileId,
                currentIntent = context.currentIntent.name,
                currentPhase = context.currentState.phase.name,
                engagementLevel = context.currentState.engagementLevel.name,
                preferences = PreferencesDto.from(context.preferences)
            )
        )
    }
    
    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "세션 정보 조회", description = "컨텍스트 세션 정보 조회")
    fun getSession(@PathVariable sessionId: String): ApiResponse<SessionDetailResponse> {
        val context = contextManager.getContext(sessionId)
            ?: return ApiResponse.error(org.springframework.http.HttpStatus.NOT_FOUND, "Session not found")
        
        val signals = contextManager.analyzeIntent(sessionId, "")
        
        return ApiResponse.success(
            SessionDetailResponse(
                sessionId = context.sessionId,
                profileId = context.profileId,
                currentIntent = context.currentIntent.name,
                currentPhase = context.currentState.phase.name,
                engagementLevel = context.currentState.engagementLevel.name,
                sessionDuration = context.currentState.sessionDuration,
                questionsAsked = context.currentState.questionsAsked,
                tasksCompleted = context.currentState.tasksCompleted,
                currentTopic = context.currentState.currentTopic,
                recentInteractions = context.getRecentInteractions(10).map { interaction ->
                    InteractionDto(
                        timestamp = interaction.timestamp.toString(),
                        intent = interaction.intent.name,
                        action = interaction.action,
                        result = interaction.result,
                        confidence = interaction.confidence
                    )
                },
                preferences = PreferencesDto.from(context.preferences)
            )
        )
    }
    
    @PutMapping("/sessions/{sessionId}/interact")
    @Operation(summary = "상호작용 기록", description = "사용자 상호작용 기록 및 컨텍스트 업데이트")
    fun recordInteraction(
        @PathVariable sessionId: String,
        @Valid @RequestBody request: InteractionRequest
    ): ApiResponse<InteractionResponse> {
        val updatedContext = contextManager.updateContext(
            sessionId = sessionId,
            action = request.action,
            result = request.result,
            intent = request.intent?.let { UserIntent.valueOf(it) },
            feedback = request.feedback?.let { 
                UserFeedback(
                    rating = it.rating,
                    helpful = it.helpful,
                    comment = it.comment
                )
            }
        ) ?: return ApiResponse.error(org.springframework.http.HttpStatus.NOT_FOUND, "Session not found")
        
        return ApiResponse.success(
            InteractionResponse(
                sessionId = sessionId,
                updatedIntent = updatedContext.currentIntent.name,
                updatedPhase = updatedContext.currentState.phase.name,
                engagementLevel = updatedContext.currentState.engagementLevel.name,
                confidence = updatedContext.currentState.confidence,
                message = "Interaction recorded successfully"
            )
        )
    }
    
    @PostMapping("/sessions/{sessionId}/analyze-intent")
    @Operation(summary = "의도 분석", description = "사용자 쿼리의 의도 분석")
    fun analyzeIntent(
        @PathVariable sessionId: String,
        @Valid @RequestBody request: IntentAnalysisRequest
    ): ApiResponse<IntentAnalysisResponse> {
        val result = contextManager.analyzeIntent(sessionId, request.query)
            ?: return ApiResponse.error(org.springframework.http.HttpStatus.NOT_FOUND, "Session not found")
        
        return ApiResponse.success(
            IntentAnalysisResponse(
                intent = result.intent.name,
                confidence = result.confidence,
                keywords = result.keywords,
                suggestedActions = result.suggestedActions,
                relatedIntents = result.relatedIntents.map { it.name }
            )
        )
    }
    
    @PostMapping("/sessions/{sessionId}/adjust-response")
    @Operation(summary = "응답 조정", description = "컨텍스트 기반 응답 조정")
    fun adjustResponse(
        @PathVariable sessionId: String,
        @Valid @RequestBody request: AdjustResponseRequest
    ): ApiResponse<AdjustedResponseDto> {
        val adjusted = contextManager.adjustResponse(
            sessionId = sessionId,
            baseResponse = request.baseResponse,
            responseMetadata = request.metadata
        )
        
        return ApiResponse.success(
            AdjustedResponseDto(
                originalContent = request.baseResponse,
                adjustedContent = adjusted.content,
                adjustments = adjusted.adjustments,
                style = adjusted.style?.let { style ->
                    ResponseStyleDto(
                        tone = style.tone.name,
                        length = style.length.name,
                        complexity = style.complexity.name,
                        includeExamples = style.includeExamples,
                        language = style.language.name
                    )
                },
                metadata = adjusted.metadata
            )
        )
    }
    
    @GetMapping("/sessions/{sessionId}/recommendations")
    @Operation(summary = "개인화 추천", description = "컨텍스트 기반 개인화 추천 생성")
    fun getRecommendations(@PathVariable sessionId: String): ApiResponse<RecommendationsResponse> {
        val recommendations = contextManager.getPersonalizedRecommendations(sessionId)
            ?: return ApiResponse.error(org.springframework.http.HttpStatus.NOT_FOUND, "Session not found or no recommendations available")
        
        return ApiResponse.success(
            RecommendationsResponse(
                content = recommendations.content.map { content ->
                    ContentRecommendationDto(
                        type = content.type,
                        title = content.title,
                        description = content.description,
                        priority = content.priority
                    )
                },
                actions = recommendations.actions.map { action ->
                    ActionRecommendationDto(
                        action = action.action,
                        reason = action.reason,
                        urgency = action.urgency
                    )
                },
                learning = recommendations.learning.map { learning ->
                    LearningRecommendationDto(
                        skill = learning.skill,
                        currentLevel = learning.currentLevel,
                        targetLevel = learning.targetLevel,
                        resources = learning.resources
                    )
                },
                priority = recommendations.priority.name,
                reasoning = recommendations.reasoning
            )
        )
    }
    
    @GetMapping("/profiles/{profileId}/sessions")
    @Operation(summary = "프로필 세션 목록", description = "특정 프로필의 모든 세션 조회")
    fun getProfileSessions(@PathVariable profileId: Long): ApiResponse<List<SessionSummaryDto>> {
        val sessions = contextManager.getProfileSessions(profileId)
        
        return ApiResponse.success(
            sessions.map { context ->
                SessionSummaryDto(
                    sessionId = context.sessionId,
                    currentIntent = context.currentIntent.name,
                    currentPhase = context.currentState.phase.name,
                    engagementLevel = context.currentState.engagementLevel.name,
                    lastActivity = context.currentState.lastActivity.toString(),
                    sessionDuration = context.currentState.sessionDuration,
                    interactionCount = context.interactionHistory.size
                )
            }
        )
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "컨텍스트 통계", description = "전체 컨텍스트 시스템 통계")
    fun getStatistics(): ApiResponse<ContextStatisticsDto> {
        val stats = contextManager.getContextStatistics()
        
        return ApiResponse.success(
            ContextStatisticsDto(
                activeSessions = stats.activeSessions,
                uniqueProfiles = stats.uniqueProfiles,
                intentDistribution = stats.intentDistribution.mapKeys { it.key.name },
                phaseDistribution = stats.phaseDistribution.mapKeys { it.key.name },
                engagementDistribution = stats.engagementDistribution.mapKeys { it.key.name },
                averageSessionDuration = stats.averageSessionDuration
            )
        )
    }
    
    @DeleteMapping("/cleanup")
    @Operation(summary = "비활성 세션 정리", description = "비활성 세션 자동 정리")
    fun cleanupSessions(
        @RequestParam(defaultValue = "30") inactiveMinutes: Int
    ): ApiResponse<String> {
        contextManager.cleanupInactiveSessions(inactiveMinutes)
        return ApiResponse.success("Inactive sessions cleaned up successfully")
    }
}