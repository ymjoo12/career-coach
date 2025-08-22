package com.careercoach.context.service

import com.careercoach.context.model.*
import com.careercoach.domain.profile.repository.ProfileRepository
import com.careercoach.domain.profile.repository.TechnicalSkillRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

@Service
class ContextManager(
    private val contextAnalyzer: ContextAnalyzer,
    private val profileRepository: ProfileRepository,
    private val technicalSkillRepository: TechnicalSkillRepository
) {
    
    // In-memory context storage (could be moved to Redis in production)
    private val contextStore = ConcurrentHashMap<String, UserContext>()
    private val sessionStore = ConcurrentHashMap<Long, MutableList<String>>()
    
    /**
     * 새로운 컨텍스트 세션 생성
     */
    fun createContext(profileId: Long, initialIntent: UserIntent? = null): UserContext {
        val sessionId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()
        
        val context = UserContext(
            profileId = profileId,
            sessionId = sessionId,
            currentIntent = initialIntent ?: UserIntent.UNKNOWN,
            interactionHistory = emptyList(),
            preferences = loadUserPreferences(profileId),
            currentState = ContextState(
                phase = InteractionPhase.INITIAL_ASSESSMENT,
                confidence = 0.5,
                engagementLevel = EngagementLevel.MEDIUM,
                lastActivity = now,
                sessionDuration = 0,
                questionsAsked = 0,
                tasksCompleted = 0
            )
        )
        
        contextStore[sessionId] = context
        sessionStore.computeIfAbsent(profileId) { mutableListOf() }.add(sessionId)
        
        return context
    }
    
    /**
     * 컨텍스트 업데이트
     */
    fun updateContext(
        sessionId: String,
        action: String,
        result: String? = null,
        intent: UserIntent? = null,
        feedback: UserFeedback? = null
    ): UserContext? {
        val context = contextStore[sessionId] ?: return null
        val now = LocalDateTime.now()
        
        // Add interaction record
        val interaction = InteractionRecord(
            timestamp = now,
            intent = intent ?: context.currentIntent,
            action = action,
            result = result,
            confidence = calculateActionConfidence(action, result),
            responseTime = null,
            feedback = feedback
        )
        
        val updatedHistory = context.interactionHistory + interaction
        
        // Update state
        val sessionDuration = java.time.Duration.between(
            context.currentState.lastActivity, 
            now
        ).toMillis() + context.currentState.sessionDuration
        
        val updatedState = context.currentState.copy(
            phase = determinePhase(updatedHistory),
            confidence = calculateStateConfidence(updatedHistory),
            engagementLevel = contextAnalyzer.analyzeEngagement(
                updatedHistory, 
                sessionDuration
            ).level,
            lastActivity = now,
            sessionDuration = sessionDuration,
            questionsAsked = if (action.contains("question")) 
                context.currentState.questionsAsked + 1 
                else context.currentState.questionsAsked,
            tasksCompleted = if (result != null && result.contains("completed"))
                context.currentState.tasksCompleted + 1
                else context.currentState.tasksCompleted,
            currentTopic = extractTopic(action)
        )
        
        val updatedContext = context.copy(
            currentIntent = intent ?: context.currentIntent,
            interactionHistory = updatedHistory,
            currentState = updatedState
        )
        
        contextStore[sessionId] = updatedContext
        return updatedContext
    }
    
    /**
     * 컨텍스트 기반 응답 조정
     */
    fun adjustResponse(
        sessionId: String,
        baseResponse: String,
        responseMetadata: Map<String, Any> = emptyMap()
    ): ContextualResponse {
        val context = contextStore[sessionId] ?: return ContextualResponse(
            content = baseResponse,
            style = null,
            adjustments = emptyList(),
            metadata = responseMetadata
        )
        
        val signals = contextAnalyzer.detectContextSignals(
            context, 
            context.getRecentInteractions()
        )
        
        val style = contextAnalyzer.determineResponseStyle(context, signals)
        val adjustedContent = applyStyleAdjustments(baseResponse, style, signals)
        
        val adjustments = mutableListOf<String>()
        
        // Apply adjustments based on context
        if (signals.isFirstTime) {
            adjustments.add("Added welcome message for first-time user")
        }
        
        if (signals.needsGuidance) {
            adjustments.add("Added guidance hints")
        }
        
        if (signals.hasUrgentNeed) {
            adjustments.add("Prioritized urgent information")
        }
        
        if (signals.prefersConcise) {
            adjustments.add("Condensed response for brevity")
        }
        
        if (signals.prefersDetailed) {
            adjustments.add("Added detailed explanations")
        }
        
        return ContextualResponse(
            content = adjustedContent,
            style = style,
            adjustments = adjustments,
            metadata = responseMetadata + mapOf(
                "signals" to signals,
                "engagement_level" to context.currentState.engagementLevel
            )
        )
    }
    
    /**
     * 컨텍스트 기반 추천 생성
     */
    fun getPersonalizedRecommendations(sessionId: String): PersonalizedRecommendations? {
        val context = contextStore[sessionId] ?: return null
        
        val profile = profileRepository.findById(context.profileId).orElse(null) 
            ?: return null
        
        val skills = technicalSkillRepository.findByProfileId(context.profileId)
        
        return contextAnalyzer.generatePersonalizedRecommendations(
            context, profile, skills
        )
    }
    
    /**
     * 의도 분석
     */
    fun analyzeIntent(
        sessionId: String,
        query: String
    ): IntentAnalysisResult? {
        val context = contextStore[sessionId] ?: return null
        val profile = profileRepository.findById(context.profileId).orElse(null)
        
        return contextAnalyzer.analyzeIntent(
            query,
            context.interactionHistory,
            profile
        )
    }
    
    /**
     * 컨텍스트 검색
     */
    fun getContext(sessionId: String): UserContext? {
        return contextStore[sessionId]
    }
    
    /**
     * 프로필별 모든 세션 검색
     */
    fun getProfileSessions(profileId: Long): List<UserContext> {
        val sessionIds = sessionStore[profileId] ?: return emptyList()
        return sessionIds.mapNotNull { contextStore[it] }
    }
    
    /**
     * 세션 정리
     */
    fun cleanupInactiveSessions(inactiveMinutes: Int = 30) {
        val cutoff = LocalDateTime.now().minusMinutes(inactiveMinutes.toLong())
        
        contextStore.entries.removeIf { (sessionId, context) ->
            val isInactive = context.currentState.lastActivity.isBefore(cutoff)
            if (isInactive) {
                sessionStore[context.profileId]?.remove(sessionId)
            }
            isInactive
        }
    }
    
    /**
     * 컨텍스트 통계
     */
    fun getContextStatistics(): ContextStatistics {
        val activeSessions = contextStore.size
        val uniqueProfiles = sessionStore.size
        
        val intentDistribution = contextStore.values
            .groupBy { it.currentIntent }
            .mapValues { it.value.size }
        
        val phaseDistribution = contextStore.values
            .groupBy { it.currentState.phase }
            .mapValues { it.value.size }
        
        val engagementDistribution = contextStore.values
            .groupBy { it.currentState.engagementLevel }
            .mapValues { it.value.size }
        
        val avgSessionDuration = contextStore.values
            .map { it.currentState.sessionDuration }
            .average()
        
        return ContextStatistics(
            activeSessions = activeSessions,
            uniqueProfiles = uniqueProfiles,
            intentDistribution = intentDistribution,
            phaseDistribution = phaseDistribution,
            engagementDistribution = engagementDistribution,
            averageSessionDuration = avgSessionDuration
        )
    }
    
    // Private helper methods
    
    private fun loadUserPreferences(profileId: Long): UserPreferences {
        // Load from database or use defaults
        // This would typically load saved preferences from a database
        return UserPreferences()
    }
    
    private fun calculateActionConfidence(action: String, result: String?): Double {
        // Simple confidence calculation based on action success
        return if (result != null && !result.contains("error")) 0.8 else 0.5
    }
    
    private fun determinePhase(history: List<InteractionRecord>): InteractionPhase {
        return when (history.size) {
            in 0..2 -> InteractionPhase.INITIAL_ASSESSMENT
            in 3..5 -> InteractionPhase.EXPLORATION
            in 6..10 -> InteractionPhase.DEEP_DIVE
            in 11..15 -> InteractionPhase.ACTION_PLANNING
            in 16..20 -> InteractionPhase.EXECUTION
            else -> InteractionPhase.REVIEW
        }
    }
    
    private fun calculateStateConfidence(history: List<InteractionRecord>): Double {
        if (history.isEmpty()) return 0.5
        
        val recentConfidences = history.takeLast(5).map { it.confidence }
        return recentConfidences.average()
    }
    
    private fun extractTopic(action: String): String? {
        // Extract main topic from action
        return when {
            action.contains("interview") -> "Interview Preparation"
            action.contains("skill") -> "Skill Assessment"
            action.contains("career") -> "Career Planning"
            action.contains("learning") -> "Learning Path"
            else -> null
        }
    }
    
    private fun applyStyleAdjustments(
        content: String,
        style: ResponseStyle,
        signals: ContextSignals
    ): String {
        var adjusted = content
        
        // Add welcome message for first-time users
        if (signals.isFirstTime) {
            adjusted = "안녕하세요! Career Coach에 오신 것을 환영합니다. \n\n$adjusted"
        }
        
        // Add urgency indicator
        if (signals.hasUrgentNeed) {
            adjusted = "⚡ 빠른 도움이 필요하신 것 같네요. 핵심 정보를 먼저 알려드립니다.\n\n$adjusted"
        }
        
        // Adjust for language preference
        when (style.language) {
            Language.ENGLISH -> {
                // Would translate or provide English version
            }
            Language.BILINGUAL -> {
                // Would provide both Korean and English
            }
            else -> {
                // Keep Korean
            }
        }
        
        // Adjust length
        when (style.length) {
            ResponseLength.SHORT -> {
                // Truncate or summarize
                if (adjusted.length > 500) {
                    adjusted = adjusted.take(500) + "..."
                }
            }
            ResponseLength.DETAILED -> {
                // Add more details
                adjusted = "$adjusted\n\n더 자세한 정보가 필요하시면 말씀해주세요."
            }
            else -> {
                // Keep medium length
            }
        }
        
        // Add emoji if casual style
        if (style.useEmoji) {
            adjusted = adjusted
                .replace("좋습니다", "좋습니다 👍")
                .replace("축하합니다", "축하합니다 🎉")
                .replace("주의", "⚠️ 주의")
        }
        
        return adjusted
    }
}

// Supporting data classes

data class ContextualResponse(
    val content: String,
    val style: ResponseStyle?,
    val adjustments: List<String>,
    val metadata: Map<String, Any>
)

data class ContextStatistics(
    val activeSessions: Int,
    val uniqueProfiles: Int,
    val intentDistribution: Map<UserIntent, Int>,
    val phaseDistribution: Map<InteractionPhase, Int>,
    val engagementDistribution: Map<EngagementLevel, Int>,
    val averageSessionDuration: Double
)