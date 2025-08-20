package com.careercoach.interview.controller

import com.careercoach.common.dto.ApiResponse
import com.careercoach.interview.dto.*
import com.careercoach.interview.service.InterviewQuestionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Interview Questions", description = "면접 질문 생성 API")
@RestController
@RequestMapping("/api/v1/interview")
class InterviewQuestionController(
    private val interviewQuestionService: InterviewQuestionService
) {
    
    private val logger = KotlinLogging.logger {}
    
    @Operation(
        summary = "면접 질문 생성",
        description = "프로필 정보를 기반으로 맞춤형 면접 질문을 생성합니다. 2단계 생성 프로세스를 통해 포지션 특화 질문을 제공합니다."
    )
    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.OK)
    fun generateQuestions(
        @Valid @RequestBody request: InterviewQuestionRequest
    ): ApiResponse<InterviewQuestionResponse> {
        logger.info { "Generating interview questions for profile ${request.profileId}" }
        
        val response = interviewQuestionService.generateInterviewQuestions(request)
        
        return ApiResponse.success(response)
    }
    
    @Operation(
        summary = "후속 질문 생성",
        description = "사용자의 답변을 기반으로 심화 후속 질문을 생성합니다."
    )
    @PostMapping("/questions/follow-up")
    @ResponseStatus(HttpStatus.OK)
    fun generateFollowUpQuestions(
        @Valid @RequestBody request: FollowUpQuestionRequest
    ): ApiResponse<FollowUpQuestionResponse> {
        logger.info { "Generating follow-up questions for profile ${request.profileId}" }
        
        val response = interviewQuestionService.generateFollowUpQuestions(request)
        
        return ApiResponse.success(response)
    }
    
    @Operation(
        summary = "특정 포지션 면접 질문 생성",
        description = "프로필 ID와 타겟 포지션을 지정하여 빠르게 면접 질문을 생성합니다."
    )
    @GetMapping("/profiles/{profileId}/questions")
    @ResponseStatus(HttpStatus.OK)
    fun generateQuestionsForProfile(
        @PathVariable profileId: Long,
        @RequestParam(name = "position") targetPosition: String,
        @RequestParam(required = false) company: String?,
        @RequestParam(defaultValue = "5") count: Int,
        @RequestParam(defaultValue = "MEDIUM") difficulty: String
    ): ApiResponse<InterviewQuestionResponse> {
        logger.info { "Quick generating questions for profile $profileId, position: $targetPosition" }
        
        val request = InterviewQuestionRequest(
            profileId = profileId,
            targetPosition = targetPosition,
            targetCompany = company,
            questionCount = count,
            difficulty = QuestionDifficulty.valueOf(difficulty.uppercase())
        )
        
        val response = interviewQuestionService.generateInterviewQuestions(request)
        
        return ApiResponse.success(response)
    }
}