package com.careercoach.interview.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class InterviewQuestionResponse(
    @JsonProperty("profile_id")
    val profileId: Long,
    
    @JsonProperty("target_position")
    val targetPosition: String,
    
    @JsonProperty("target_company")
    val targetCompany: String?,
    
    @JsonProperty("questions")
    val questions: List<InterviewQuestion>,
    
    @JsonProperty("generated_at")
    val generatedAt: LocalDateTime = LocalDateTime.now(),
    
    @JsonProperty("total_questions")
    val totalQuestions: Int = questions.size,
    
    @JsonProperty("difficulty_distribution")
    val difficultyDistribution: Map<String, Int>,
    
    @JsonProperty("type_distribution")
    val typeDistribution: Map<String, Int>
)

data class InterviewQuestion(
    @JsonProperty("id")
    val id: Int,
    
    @JsonProperty("category")
    val category: String,
    
    @JsonProperty("type")
    val type: QuestionType,
    
    @JsonProperty("difficulty")
    val difficulty: QuestionDifficulty,
    
    @JsonProperty("question")
    val question: String,
    
    @JsonProperty("context")
    val context: String? = null,
    
    @JsonProperty("expected_answer")
    val expectedAnswer: String,
    
    @JsonProperty("evaluation_criteria")
    val evaluationCriteria: List<String>,
    
    @JsonProperty("follow_up_questions")
    val followUpQuestions: List<String> = emptyList(),
    
    @JsonProperty("time_estimate_minutes")
    val timeEstimateMinutes: Int = 5,
    
    @JsonProperty("tags")
    val tags: List<String> = emptyList()
)

data class FollowUpQuestionRequest(
    @JsonProperty("profile_id")
    val profileId: Long,
    
    @JsonProperty("original_question")
    val originalQuestion: String,
    
    @JsonProperty("user_answer")
    val userAnswer: String,
    
    @JsonProperty("question_count")
    val questionCount: Int = 2
)

data class FollowUpQuestionResponse(
    @JsonProperty("original_question")
    val originalQuestion: String,
    
    @JsonProperty("user_answer")
    val userAnswer: String,
    
    @JsonProperty("follow_up_questions")
    val followUpQuestions: List<FollowUpQuestion>
)

data class FollowUpQuestion(
    @JsonProperty("question")
    val question: String,
    
    @JsonProperty("purpose")
    val purpose: String,
    
    @JsonProperty("expected_depth")
    val expectedDepth: String
)