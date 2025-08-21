package com.careercoach.learning.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class LearningPathResponse(
    @JsonProperty("profile_id")
    val profileId: Long,
    
    @JsonProperty("target_position")
    val targetPosition: String,
    
    @JsonProperty("target_company")
    val targetCompany: String?,
    
    @JsonProperty("roadmap")
    val roadmap: LearningRoadmap,
    
    @JsonProperty("skill_gap_analysis")
    val skillGapAnalysis: SkillGapAnalysis,
    
    @JsonProperty("estimated_completion_months")
    val estimatedCompletionMonths: Int,
    
    @JsonProperty("total_estimated_cost")
    val totalEstimatedCost: Int?,
    
    @JsonProperty("generated_at")
    val generatedAt: LocalDateTime = LocalDateTime.now()
)

data class LearningRoadmap(
    @JsonProperty("total_duration_months")
    val totalDurationMonths: Int,
    
    @JsonProperty("phases")
    val phases: List<LearningPhase>,
    
    @JsonProperty("weekly_schedule")
    val weeklySchedule: WeeklySchedule?,
    
    @JsonProperty("milestones")
    val milestones: List<Milestone>
)

data class LearningPhase(
    @JsonProperty("phase_number")
    val phaseNumber: Int,
    
    @JsonProperty("title")
    val title: String,
    
    @JsonProperty("description")
    val description: String,
    
    @JsonProperty("duration_weeks")
    val durationWeeks: Int,
    
    @JsonProperty("skills")
    val skills: List<String>,
    
    @JsonProperty("resources")
    val resources: List<LearningResource>,
    
    @JsonProperty("projects")
    val projects: List<PracticeProject>,
    
    @JsonProperty("completion_criteria")
    val completionCriteria: List<String>
)

data class LearningResource(
    @JsonProperty("type")
    val type: ResourceType,
    
    @JsonProperty("title")
    val title: String,
    
    @JsonProperty("description")
    val description: String?,
    
    @JsonProperty("url")
    val url: String?,
    
    @JsonProperty("platform")
    val platform: String?,
    
    @JsonProperty("estimated_hours")
    val estimatedHours: Int,
    
    @JsonProperty("cost")
    val cost: Int?,
    
    @JsonProperty("difficulty")
    val difficulty: String,
    
    @JsonProperty("is_free")
    val isFree: Boolean,
    
    @JsonProperty("language")
    val language: String = "ko"
)

enum class ResourceType {
    COURSE,
    BOOK,
    TUTORIAL,
    DOCUMENTATION,
    VIDEO,
    ARTICLE,
    PRACTICE
}

data class PracticeProject(
    @JsonProperty("title")
    val title: String,
    
    @JsonProperty("description")
    val description: String,
    
    @JsonProperty("technologies")
    val technologies: List<String>,
    
    @JsonProperty("estimated_hours")
    val estimatedHours: Int,
    
    @JsonProperty("difficulty")
    val difficulty: String,
    
    @JsonProperty("learning_outcomes")
    val learningOutcomes: List<String>
)

data class WeeklySchedule(
    @JsonProperty("monday")
    val monday: List<String>,
    
    @JsonProperty("tuesday")
    val tuesday: List<String>,
    
    @JsonProperty("wednesday")
    val wednesday: List<String>,
    
    @JsonProperty("thursday")
    val thursday: List<String>,
    
    @JsonProperty("friday")
    val friday: List<String>,
    
    @JsonProperty("saturday")
    val saturday: List<String>,
    
    @JsonProperty("sunday")
    val sunday: List<String>
)

data class Milestone(
    @JsonProperty("week")
    val week: Int,
    
    @JsonProperty("title")
    val title: String,
    
    @JsonProperty("description")
    val description: String,
    
    @JsonProperty("deliverables")
    val deliverables: List<String>
)

data class SkillGapAnalysis(
    @JsonProperty("current_skills")
    val currentSkills: List<SkillAssessment>,
    
    @JsonProperty("required_skills")
    val requiredSkills: List<SkillRequirement>,
    
    @JsonProperty("gap_skills")
    val gapSkills: List<SkillGap>,
    
    @JsonProperty("transferable_skills")
    val transferableSkills: List<String>,
    
    @JsonProperty("priority_areas")
    val priorityAreas: List<String>
)

data class SkillAssessment(
    @JsonProperty("skill")
    val skill: String,
    
    @JsonProperty("current_level")
    val currentLevel: String,
    
    @JsonProperty("years_of_experience")
    val yearsOfExperience: Double
)

data class SkillRequirement(
    @JsonProperty("skill")
    val skill: String,
    
    @JsonProperty("required_level")
    val requiredLevel: String,
    
    @JsonProperty("importance")
    val importance: String,
    
    @JsonProperty("market_demand")
    val marketDemand: String
)

data class SkillGap(
    @JsonProperty("skill")
    val skill: String,
    
    @JsonProperty("current_level")
    val currentLevel: String?,
    
    @JsonProperty("required_level")
    val requiredLevel: String,
    
    @JsonProperty("gap_size")
    val gapSize: String,
    
    @JsonProperty("estimated_time_to_learn")
    val estimatedTimeToLearn: Int,
    
    @JsonProperty("priority")
    val priority: String
)