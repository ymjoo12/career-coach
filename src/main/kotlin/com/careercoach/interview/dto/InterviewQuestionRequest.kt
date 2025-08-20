package com.careercoach.interview.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class InterviewQuestionRequest(
    @field:NotNull(message = "Profile ID is required")
    @JsonProperty("profile_id")
    val profileId: Long,
    
    @field:NotNull(message = "Target position is required")
    @JsonProperty("target_position")
    val targetPosition: String,
    
    @JsonProperty("target_company")
    val targetCompany: String? = null,
    
    @field:Min(1, message = "Question count must be at least 1")
    @field:Max(20, message = "Question count cannot exceed 20")
    @JsonProperty("question_count")
    val questionCount: Int = 5,
    
    @JsonProperty("difficulty")
    val difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
    
    @JsonProperty("question_types")
    val questionTypes: List<QuestionType> = listOf(
        QuestionType.TECHNICAL,
        QuestionType.BEHAVIORAL,
        QuestionType.SITUATIONAL
    ),
    
    @JsonProperty("focus_areas")
    val focusAreas: List<String> = emptyList(),
    
    @JsonProperty("include_follow_ups")
    val includeFollowUps: Boolean = true
)

enum class QuestionDifficulty {
    EASY, MEDIUM, HARD, MIXED
}

enum class QuestionType {
    TECHNICAL,
    BEHAVIORAL,
    SITUATIONAL,
    SYSTEM_DESIGN,
    CODING,
    DOMAIN_SPECIFIC
}