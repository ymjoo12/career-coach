package com.careercoach.context.service

import com.careercoach.context.model.*
import com.careercoach.domain.profile.entity.Profile
import com.careercoach.domain.profile.entity.TechnicalSkill
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.min

@Service
class ContextAnalyzer {
    
    /**
     * 사용자 의도 분석
     */
    fun analyzeIntent(
        query: String,
        history: List<InteractionRecord>,
        profile: Profile?
    ): IntentAnalysisResult {
        val keywords = extractKeywords(query.lowercase())
        val historicalIntent = getHistoricalIntent(history)
        
        val intent = when {
            containsInterviewKeywords(keywords) -> UserIntent.INTERVIEW_PREPARATION
            containsSkillKeywords(keywords) -> UserIntent.SKILL_ASSESSMENT
            containsCareerKeywords(keywords) -> UserIntent.CAREER_PLANNING
            containsLearningKeywords(keywords) -> UserIntent.LEARNING_PATH
            containsResumeKeywords(keywords) -> UserIntent.RESUME_IMPROVEMENT
            containsCompanyKeywords(keywords) -> UserIntent.COMPANY_RESEARCH
            containsSalaryKeywords(keywords) -> UserIntent.SALARY_NEGOTIATION
            else -> historicalIntent ?: UserIntent.GENERAL_INQUIRY
        }
        
        val confidence = calculateIntentConfidence(intent, keywords, history)
        val suggestedActions = suggestActions(intent, profile)
        
        return IntentAnalysisResult(
            intent = intent,
            confidence = confidence,
            keywords = keywords,
            suggestedActions = suggestedActions,
            relatedIntents = findRelatedIntents(intent)
        )
    }
    
    /**
     * 컨텍스트 신호 감지
     */
    fun detectContextSignals(
        context: UserContext,
        recentActivity: List<InteractionRecord>
    ): ContextSignals {
        val isFirstTime = context.interactionHistory.size <= 1
        val isReturningUser = !isFirstTime && 
            context.interactionHistory.any { 
                it.timestamp.isBefore(LocalDateTime.now().minusDays(1))
            }
        
        val hasUrgentNeed = detectUrgency(recentActivity, context.preferences)
        val isConfused = detectConfusion(recentActivity)
        val needsGuidance = detectGuidanceNeed(recentActivity, context.currentState)
        val isExperienced = detectExperience(context)
        
        val prefersConcise = context.preferences.communicationStyle == CommunicationStyle.DIRECT ||
            recentActivity.any { it.action.contains("summary") || it.action.contains("brief") }
        
        val prefersDetailed = context.preferences.communicationStyle == CommunicationStyle.PROFESSIONAL ||
            recentActivity.any { it.action.contains("detail") || it.action.contains("explain") }
        
        val timeToDecision = context.preferences.timeConstraints?.deadline?.let {
            ChronoUnit.DAYS.between(LocalDateTime.now(), it).toInt()
        }
        
        return ContextSignals(
            isFirstTime = isFirstTime,
            isReturningUser = isReturningUser,
            hasUrgentNeed = hasUrgentNeed,
            isConfused = isConfused,
            needsGuidance = needsGuidance,
            isExperienced = isExperienced,
            prefersConcise = prefersConcise,
            prefersDetailed = prefersDetailed,
            timeToDecision = timeToDecision
        )
    }
    
    /**
     * 개인화 추천 생성
     */
    fun generatePersonalizedRecommendations(
        context: UserContext,
        profile: Profile,
        skills: List<TechnicalSkill>
    ): PersonalizedRecommendations {
        val signals = detectContextSignals(context, context.getRecentInteractions())
        
        val contentRecommendations = generateContentRecommendations(
            context, profile, skills, signals
        )
        
        val actionRecommendations = generateActionRecommendations(
            context, signals
        )
        
        val learningRecommendations = generateLearningRecommendations(
            context, skills, signals
        )
        
        val priority = calculateRecommendationPriority(context, signals)
        
        return PersonalizedRecommendations(
            content = contentRecommendations,
            actions = actionRecommendations,
            learning = learningRecommendations,
            priority = priority,
            reasoning = generateRecommendationReasoning(context, signals)
        )
    }
    
    /**
     * 참여도 분석
     */
    fun analyzeEngagement(
        interactions: List<InteractionRecord>,
        sessionDuration: Long
    ): EngagementAnalysis {
        if (interactions.isEmpty()) {
            return EngagementAnalysis(
                level = EngagementLevel.LOW,
                score = 0.0,
                trends = emptyList(),
                suggestions = listOf("첫 상호작용을 시작해보세요")
            )
        }
        
        val interactionRate = interactions.size.toDouble() / (sessionDuration / 60000.0) // per minute
        val avgResponseTime = interactions.mapNotNull { it.responseTime }.average()
        val feedbackRate = interactions.count { it.feedback != null }.toDouble() / interactions.size
        val positivityRate = interactions
            .mapNotNull { it.feedback }
            .count { it.helpful }
            .toDouble() / interactions.count { it.feedback != null }
        
        val score = calculateEngagementScore(
            interactionRate, avgResponseTime, feedbackRate, positivityRate
        )
        
        val level = when {
            score >= 0.8 -> EngagementLevel.VERY_HIGH
            score >= 0.6 -> EngagementLevel.HIGH
            score >= 0.4 -> EngagementLevel.MEDIUM
            else -> EngagementLevel.LOW
        }
        
        val trends = analyzeEngagementTrends(interactions)
        val suggestions = generateEngagementSuggestions(level, trends)
        
        return EngagementAnalysis(
            level = level,
            score = score,
            trends = trends,
            suggestions = suggestions
        )
    }
    
    /**
     * 응답 스타일 결정
     */
    fun determineResponseStyle(
        context: UserContext,
        signals: ContextSignals
    ): ResponseStyle {
        val tone = when (context.preferences.communicationStyle) {
            CommunicationStyle.CASUAL -> ResponseTone.FRIENDLY
            CommunicationStyle.PROFESSIONAL -> ResponseTone.FORMAL
            CommunicationStyle.ENCOURAGING -> ResponseTone.SUPPORTIVE
            CommunicationStyle.DIRECT -> ResponseTone.CONCISE
        }
        
        val length = when {
            signals.prefersConcise -> ResponseLength.SHORT
            signals.prefersDetailed -> ResponseLength.DETAILED
            else -> ResponseLength.MEDIUM
        }
        
        val complexity = when (context.preferences.difficulty) {
            DifficultyLevel.BEGINNER -> ResponseComplexity.SIMPLE
            DifficultyLevel.INTERMEDIATE -> ResponseComplexity.MODERATE
            DifficultyLevel.ADVANCED -> ResponseComplexity.COMPLEX
            DifficultyLevel.EXPERT -> ResponseComplexity.TECHNICAL
        }
        
        val includeExamples = context.preferences.learningStyle in listOf(
            LearningStyle.PRACTICAL, LearningStyle.VISUAL
        )
        
        val includeVisuals = context.preferences.learningStyle == LearningStyle.VISUAL
        
        return ResponseStyle(
            tone = tone,
            length = length,
            complexity = complexity,
            includeExamples = includeExamples,
            includeVisuals = includeVisuals,
            useEmoji = context.preferences.communicationStyle == CommunicationStyle.CASUAL,
            language = context.preferences.language
        )
    }
    
    // Private helper methods
    
    private fun extractKeywords(query: String): List<String> {
        val stopWords = setOf("the", "is", "at", "which", "on", "를", "을", "이", "가", "에", "에서")
        return query.split(" ")
            .filter { it.length > 2 && it !in stopWords }
            .distinct()
    }
    
    private fun containsInterviewKeywords(keywords: List<String>): Boolean {
        val interviewWords = setOf("면접", "interview", "질문", "question", "준비", "prepare")
        return keywords.any { it in interviewWords }
    }
    
    private fun containsSkillKeywords(keywords: List<String>): Boolean {
        val skillWords = setOf("스킬", "skill", "기술", "능력", "역량", "평가", "assessment")
        return keywords.any { it in skillWords }
    }
    
    private fun containsCareerKeywords(keywords: List<String>): Boolean {
        val careerWords = setOf("커리어", "career", "경력", "진로", "성장", "growth")
        return keywords.any { it in careerWords }
    }
    
    private fun containsLearningKeywords(keywords: List<String>): Boolean {
        val learningWords = setOf("학습", "learning", "공부", "study", "교육", "강의", "course")
        return keywords.any { it in learningWords }
    }
    
    private fun containsResumeKeywords(keywords: List<String>): Boolean {
        val resumeWords = setOf("이력서", "resume", "cv", "자소서", "포트폴리오")
        return keywords.any { it in resumeWords }
    }
    
    private fun containsCompanyKeywords(keywords: List<String>): Boolean {
        val companyWords = setOf("회사", "company", "기업", "네이버", "카카오", "쿠팡")
        return keywords.any { it in companyWords }
    }
    
    private fun containsSalaryKeywords(keywords: List<String>): Boolean {
        val salaryWords = setOf("연봉", "salary", "급여", "페이", "협상", "negotiation")
        return keywords.any { it in salaryWords }
    }
    
    private fun getHistoricalIntent(history: List<InteractionRecord>): UserIntent? {
        return history.takeLast(5)
            .groupBy { it.intent }
            .maxByOrNull { it.value.size }
            ?.key
    }
    
    private fun calculateIntentConfidence(
        intent: UserIntent,
        keywords: List<String>,
        history: List<InteractionRecord>
    ): Double {
        val keywordScore = min(keywords.size * 0.2, 1.0)
        val historyScore = if (history.any { it.intent == intent }) 0.3 else 0.0
        return min(keywordScore + historyScore + 0.5, 1.0)
    }
    
    private fun suggestActions(intent: UserIntent, profile: Profile?): List<String> {
        return when (intent) {
            UserIntent.INTERVIEW_PREPARATION -> listOf(
                "면접 질문 생성",
                "모의 면접 시작",
                "답변 전략 학습"
            )
            UserIntent.SKILL_ASSESSMENT -> listOf(
                "기술 스택 평가",
                "스킬 갭 분석",
                "개선 계획 수립"
            )
            UserIntent.CAREER_PLANNING -> listOf(
                "커리어 로드맵 작성",
                "목표 설정",
                "성장 전략 수립"
            )
            UserIntent.LEARNING_PATH -> listOf(
                "학습 경로 생성",
                "추천 강의 확인",
                "학습 일정 계획"
            )
            else -> listOf("프로필 업데이트", "질문하기", "도움말 보기")
        }
    }
    
    private fun findRelatedIntents(intent: UserIntent): List<UserIntent> {
        return when (intent) {
            UserIntent.INTERVIEW_PREPARATION -> listOf(
                UserIntent.SKILL_ASSESSMENT,
                UserIntent.COMPANY_RESEARCH
            )
            UserIntent.CAREER_PLANNING -> listOf(
                UserIntent.LEARNING_PATH,
                UserIntent.SKILL_ASSESSMENT
            )
            else -> emptyList()
        }
    }
    
    private fun detectUrgency(
        activity: List<InteractionRecord>,
        preferences: UserPreferences
    ): Boolean {
        val hasDeadline = preferences.timeConstraints?.deadline?.let {
            ChronoUnit.DAYS.between(LocalDateTime.now(), it) <= 7
        } ?: false
        
        val frequentActivity = activity.size > 10
        return hasDeadline || frequentActivity
    }
    
    private fun detectConfusion(activity: List<InteractionRecord>): Boolean {
        val lowConfidenceCount = activity.count { it.confidence < 0.5 }
        return lowConfidenceCount > activity.size * 0.3
    }
    
    private fun detectGuidanceNeed(
        activity: List<InteractionRecord>,
        state: ContextState
    ): Boolean {
        return state.questionsAsked < 3 || 
               state.phase == InteractionPhase.INITIAL_ASSESSMENT ||
               activity.isEmpty()
    }
    
    private fun detectExperience(context: UserContext): Boolean {
        return context.interactionHistory.size > 20 &&
               context.currentState.tasksCompleted > 5
    }
    
    private fun calculateEngagementScore(
        interactionRate: Double,
        avgResponseTime: Double,
        feedbackRate: Double,
        positivityRate: Double
    ): Double {
        val interactionScore = min(interactionRate / 2.0, 1.0) * 0.3
        val responseScore = (1.0 - min(avgResponseTime / 10000.0, 1.0)) * 0.2
        val feedbackScore = feedbackRate * 0.2
        val positivityScore = positivityRate * 0.3
        
        return interactionScore + responseScore + feedbackScore + positivityScore
    }
    
    private fun analyzeEngagementTrends(
        interactions: List<InteractionRecord>
    ): List<EngagementTrend> {
        val trends = mutableListOf<EngagementTrend>()
        
        if (interactions.size >= 10) {
            val recentConfidence = interactions.takeLast(5).map { it.confidence }.average()
            val oldConfidence = interactions.take(5).map { it.confidence }.average()
            
            if (recentConfidence > oldConfidence * 1.2) {
                trends.add(EngagementTrend.INCREASING)
            } else if (recentConfidence < oldConfidence * 0.8) {
                trends.add(EngagementTrend.DECREASING)
            } else {
                trends.add(EngagementTrend.STABLE)
            }
        }
        
        return trends
    }
    
    private fun generateEngagementSuggestions(
        level: EngagementLevel,
        trends: List<EngagementTrend>
    ): List<String> {
        return when (level) {
            EngagementLevel.LOW -> listOf(
                "더 구체적인 질문을 해보세요",
                "프로필을 업데이트하여 맞춤 추천을 받아보세요"
            )
            EngagementLevel.MEDIUM -> listOf(
                "학습 목표를 설정해보세요",
                "모의 면접을 시도해보세요"
            )
            EngagementLevel.HIGH -> listOf(
                "심화 학습 콘텐츠를 확인해보세요",
                "다른 사용자와 경험을 공유해보세요"
            )
            EngagementLevel.VERY_HIGH -> listOf(
                "멘토링 기회를 찾아보세요",
                "고급 기능을 활용해보세요"
            )
        }
    }
    
    private fun generateContentRecommendations(
        context: UserContext,
        profile: Profile,
        skills: List<TechnicalSkill>,
        signals: ContextSignals
    ): List<ContentRecommendation> {
        val recommendations = mutableListOf<ContentRecommendation>()
        
        if (signals.isFirstTime) {
            recommendations.add(ContentRecommendation(
                type = "GUIDE",
                title = "시작 가이드",
                description = "Career Coach 활용법",
                priority = 1
            ))
        }
        
        if (signals.hasUrgentNeed) {
            recommendations.add(ContentRecommendation(
                type = "QUICK_PREP",
                title = "빠른 면접 준비",
                description = "핵심 질문과 답변 전략",
                priority = 1
            ))
        }
        
        return recommendations
    }
    
    private fun generateActionRecommendations(
        context: UserContext,
        signals: ContextSignals
    ): List<ActionRecommendation> {
        return listOf(
            ActionRecommendation(
                action = "프로필 완성도 높이기",
                reason = "더 정확한 추천을 위해",
                urgency = if (signals.hasUrgentNeed) "HIGH" else "MEDIUM"
            )
        )
    }
    
    private fun generateLearningRecommendations(
        context: UserContext,
        skills: List<TechnicalSkill>,
        signals: ContextSignals
    ): List<LearningRecommendation> {
        return skills.filter { it.level == TechnicalSkill.SkillLevel.BEGINNER }
            .map { skill ->
                LearningRecommendation(
                    skill = skill.name,
                    currentLevel = skill.level?.name ?: "UNKNOWN",
                    targetLevel = "INTERMEDIATE",
                    resources = emptyList()
                )
            }
    }
    
    private fun calculateRecommendationPriority(
        context: UserContext,
        signals: ContextSignals
    ): RecommendationPriority {
        return when {
            signals.hasUrgentNeed -> RecommendationPriority.URGENT
            signals.needsGuidance -> RecommendationPriority.HIGH
            else -> RecommendationPriority.NORMAL
        }
    }
    
    private fun generateRecommendationReasoning(
        context: UserContext,
        signals: ContextSignals
    ): String {
        return buildString {
            if (signals.isFirstTime) {
                append("처음 방문하셨네요. ")
            }
            if (signals.hasUrgentNeed) {
                append("급한 준비가 필요해 보입니다. ")
            }
            if (signals.needsGuidance) {
                append("도움이 필요하신 것 같습니다. ")
            }
            append("맞춤형 추천을 제공합니다.")
        }
    }
}

// Supporting data classes

data class IntentAnalysisResult(
    val intent: UserIntent,
    val confidence: Double,
    val keywords: List<String>,
    val suggestedActions: List<String>,
    val relatedIntents: List<UserIntent>
)

data class PersonalizedRecommendations(
    val content: List<ContentRecommendation>,
    val actions: List<ActionRecommendation>,
    val learning: List<LearningRecommendation>,
    val priority: RecommendationPriority,
    val reasoning: String
)

data class ContentRecommendation(
    val type: String,
    val title: String,
    val description: String,
    val priority: Int
)

data class ActionRecommendation(
    val action: String,
    val reason: String,
    val urgency: String
)

data class LearningRecommendation(
    val skill: String,
    val currentLevel: String,
    val targetLevel: String,
    val resources: List<String>
)

enum class RecommendationPriority {
    URGENT, HIGH, NORMAL, LOW
}

data class EngagementAnalysis(
    val level: EngagementLevel,
    val score: Double,
    val trends: List<EngagementTrend>,
    val suggestions: List<String>
)

enum class EngagementTrend {
    INCREASING, STABLE, DECREASING
}

data class ResponseStyle(
    val tone: ResponseTone,
    val length: ResponseLength,
    val complexity: ResponseComplexity,
    val includeExamples: Boolean,
    val includeVisuals: Boolean,
    val useEmoji: Boolean,
    val language: Language
)

enum class ResponseTone {
    FRIENDLY, FORMAL, SUPPORTIVE, CONCISE
}

enum class ResponseLength {
    SHORT, MEDIUM, DETAILED
}

enum class ResponseComplexity {
    SIMPLE, MODERATE, COMPLEX, TECHNICAL
}