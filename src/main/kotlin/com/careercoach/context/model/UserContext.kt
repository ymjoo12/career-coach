package com.careercoach.context.model

import java.time.LocalDateTime

/**
 * 사용자 컨텍스트 모델
 * 사용자의 현재 상태, 선호도, 상호작용 패턴을 추적
 */
data class UserContext(
    val profileId: Long,
    val sessionId: String,
    val currentIntent: UserIntent,
    val interactionHistory: List<InteractionRecord>,
    val preferences: UserPreferences,
    val currentState: ContextState,
    val metadata: Map<String, Any> = emptyMap()
) {
    fun getRecentInteractions(minutes: Int = 30): List<InteractionRecord> {
        val cutoff = LocalDateTime.now().minusMinutes(minutes.toLong())
        return interactionHistory.filter { it.timestamp.isAfter(cutoff) }
    }
    
    fun getMostFrequentIntent(): UserIntent? {
        return interactionHistory
            .groupBy { it.intent }
            .maxByOrNull { it.value.size }
            ?.key
    }
    
    fun getAverageResponseTime(): Double {
        val responseTimes = interactionHistory.mapNotNull { it.responseTime }
        return if (responseTimes.isNotEmpty()) {
            responseTimes.average()
        } else 0.0
    }
}

/**
 * 사용자 의도 분류
 */
enum class UserIntent {
    INTERVIEW_PREPARATION,    // 면접 준비
    SKILL_ASSESSMENT,        // 스킬 평가
    CAREER_PLANNING,         // 커리어 계획
    LEARNING_PATH,          // 학습 경로
    RESUME_IMPROVEMENT,     // 이력서 개선
    COMPANY_RESEARCH,       // 회사 조사
    SALARY_NEGOTIATION,     // 연봉 협상
    GENERAL_INQUIRY,        // 일반 문의
    UNKNOWN
}

/**
 * 상호작용 기록
 */
data class InteractionRecord(
    val timestamp: LocalDateTime,
    val intent: UserIntent,
    val action: String,
    val result: String?,
    val confidence: Double,
    val responseTime: Long? = null,
    val feedback: UserFeedback? = null
)

/**
 * 사용자 선호도
 */
data class UserPreferences(
    val language: Language = Language.KOREAN,
    val difficulty: DifficultyLevel = DifficultyLevel.INTERMEDIATE,
    val learningStyle: LearningStyle = LearningStyle.BALANCED,
    val communicationStyle: CommunicationStyle = CommunicationStyle.PROFESSIONAL,
    val focusAreas: List<String> = emptyList(),
    val avoidAreas: List<String> = emptyList(),
    val preferredCompanies: List<String> = emptyList(),
    val preferredPositions: List<String> = emptyList(),
    val timeConstraints: TimeConstraints? = null
)

/**
 * 컨텍스트 상태
 */
data class ContextState(
    val phase: InteractionPhase,
    val confidence: Double,
    val engagementLevel: EngagementLevel,
    val lastActivity: LocalDateTime,
    val sessionDuration: Long,
    val questionsAsked: Int = 0,
    val tasksCompleted: Int = 0,
    val currentTopic: String? = null,
    val pendingActions: List<String> = emptyList()
)

/**
 * 사용자 피드백
 */
data class UserFeedback(
    val rating: Int,  // 1-5
    val helpful: Boolean,
    val comment: String? = null,
    val improvements: List<String> = emptyList()
)

/**
 * 언어 설정
 */
enum class Language {
    KOREAN,
    ENGLISH,
    BILINGUAL
}

/**
 * 난이도 레벨
 */
enum class DifficultyLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

/**
 * 학습 스타일
 */
enum class LearningStyle {
    VISUAL,         // 시각적 학습
    PRACTICAL,      // 실습 중심
    THEORETICAL,    // 이론 중심
    BALANCED       // 균형잡힌
}

/**
 * 커뮤니케이션 스타일
 */
enum class CommunicationStyle {
    CASUAL,         // 캐주얼
    PROFESSIONAL,   // 전문적
    ENCOURAGING,    // 격려하는
    DIRECT         // 직접적
}

/**
 * 시간 제약
 */
data class TimeConstraints(
    val dailyHours: Int,
    val weeklyDays: Int,
    val preferredTimes: List<String>,
    val deadline: LocalDateTime?
)

/**
 * 상호작용 단계
 */
enum class InteractionPhase {
    INITIAL_ASSESSMENT,     // 초기 평가
    EXPLORATION,           // 탐색
    DEEP_DIVE,            // 심화
    ACTION_PLANNING,       // 실행 계획
    EXECUTION,            // 실행
    REVIEW,              // 검토
    FOLLOW_UP            // 후속 조치
}

/**
 * 참여도 레벨
 */
enum class EngagementLevel {
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}

/**
 * 컨텍스트 신호
 * 사용자의 현재 상태를 나타내는 지표
 */
data class ContextSignals(
    val isFirstTime: Boolean,
    val isReturningUser: Boolean,
    val hasUrgentNeed: Boolean,
    val isConfused: Boolean,
    val needsGuidance: Boolean,
    val isExperienced: Boolean,
    val prefersConcise: Boolean,
    val prefersDetailed: Boolean,
    val timeToDecision: Int? = null  // days
)