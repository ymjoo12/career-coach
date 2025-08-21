package com.careercoach.learning.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class LearningPathRequest(
    @field:NotNull(message = "Profile ID is required")
    @JsonProperty("profile_id")
    val profileId: Long,
    
    @field:NotNull(message = "Target position is required")
    @JsonProperty("target_position")
    val targetPosition: String,
    
    @JsonProperty("target_company")
    val targetCompany: String? = null,
    
    @JsonProperty("target_level")
    val targetLevel: CareerLevel = CareerLevel.INTERMEDIATE,
    
    @field:Min(1, message = "Daily hours must be at least 1")
    @field:Max(12, message = "Daily hours cannot exceed 12")
    @JsonProperty("daily_study_hours")
    val dailyStudyHours: Int = 2,
    
    @JsonProperty("deadline_months")
    val deadlineMonths: Int? = null,
    
    @JsonProperty("budget_monthly")
    val budgetMonthly: Int? = null,
    
    @JsonProperty("preferred_learning_style")
    val preferredLearningStyle: LearningStyle = LearningStyle.MIXED,
    
    @JsonProperty("focus_areas")
    val focusAreas: List<String> = emptyList(),
    
    @JsonProperty("exclude_topics")
    val excludeTopics: List<String> = emptyList()
)

enum class CareerLevel {
    JUNIOR,
    INTERMEDIATE,
    SENIOR,
    LEAD,
    PRINCIPAL
}

enum class LearningStyle {
    VIDEO,
    READING,
    HANDS_ON,
    MIXED
}

data class SkillGapAnalysisRequest(
    @field:NotNull(message = "Profile ID is required")
    @JsonProperty("profile_id")
    val profileId: Long,
    
    @field:NotNull(message = "Target position is required")
    @JsonProperty("target_position")
    val targetPosition: String,
    
    @JsonProperty("target_company")
    val targetCompany: String? = null
)