package com.careercoach.learning.controller

import com.careercoach.common.dto.ApiResponse
import com.careercoach.learning.dto.*
import com.careercoach.learning.service.LearningPathService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Learning Paths", description = "학습 경로 생성 API")
@RestController
@RequestMapping("/api/v1/learning")
class LearningPathController(
    private val learningPathService: LearningPathService
) {
    
    private val logger = KotlinLogging.logger {}
    
    @Operation(
        summary = "학습 경로 생성",
        description = "프로필 정보와 목표를 기반으로 맞춤형 학습 경로를 생성합니다."
    )
    @PostMapping("/paths")
    @ResponseStatus(HttpStatus.OK)
    fun generateLearningPath(
        @Valid @RequestBody request: LearningPathRequest
    ): ApiResponse<LearningPathResponse> {
        logger.info { "Generating learning path for profile ${request.profileId}" }
        
        val response = learningPathService.generateLearningPath(request)
        
        return ApiResponse.success(response)
    }
    
    @Operation(
        summary = "스킬 갭 분석",
        description = "현재 스킬과 목표 포지션 간의 스킬 갭을 분석합니다."
    )
    @PostMapping("/skill-gap")
    @ResponseStatus(HttpStatus.OK)
    fun analyzeSkillGap(
        @Valid @RequestBody request: SkillGapAnalysisRequest
    ): ApiResponse<SkillGapAnalysis> {
        logger.info { "Analyzing skill gap for profile ${request.profileId}" }
        
        val response = learningPathService.analyzeSkillGap(request)
        
        return ApiResponse.success(response)
    }
    
    @Operation(
        summary = "프로필별 학습 경로 조회",
        description = "특정 프로필에 대한 학습 경로를 빠르게 생성합니다."
    )
    @GetMapping("/profiles/{profileId}/paths")
    @ResponseStatus(HttpStatus.OK)
    fun getLearningPathForProfile(
        @PathVariable profileId: Long,
        @RequestParam(name = "position") targetPosition: String,
        @RequestParam(required = false) company: String?,
        @RequestParam(name = "daily_hours", defaultValue = "2") dailyHours: Int,
        @RequestParam(name = "deadline_months", required = false) deadlineMonths: Int?,
        @RequestParam(name = "budget", required = false) budget: Int?
    ): ApiResponse<LearningPathResponse> {
        logger.info { "Quick generating learning path for profile $profileId, position: $targetPosition" }
        
        val request = LearningPathRequest(
            profileId = profileId,
            targetPosition = targetPosition,
            targetCompany = company,
            dailyStudyHours = dailyHours,
            deadlineMonths = deadlineMonths,
            budgetMonthly = budget
        )
        
        val response = learningPathService.generateLearningPath(request)
        
        return ApiResponse.success(response)
    }
    
    @Operation(
        summary = "추천 학습 리소스 조회",
        description = "특정 스킬에 대한 추천 학습 리소스를 제공합니다."
    )
    @GetMapping("/resources/recommendations")
    @ResponseStatus(HttpStatus.OK)
    fun getRecommendedResources(
        @RequestParam skill: String,
        @RequestParam(required = false) level: String?,
        @RequestParam(name = "learning_style", required = false) style: String?,
        @RequestParam(name = "max_budget", required = false) maxBudget: Int?
    ): ApiResponse<List<LearningResource>> {
        logger.info { "Getting recommended resources for skill: $skill" }
        
        // 간단한 하드코딩된 추천 리소스 반환 (실제로는 DB나 별도 서비스에서 가져와야 함)
        val resources = listOf(
            LearningResource(
                type = ResourceType.COURSE,
                title = "$skill 완벽 가이드",
                description = "$skill 를 마스터하기 위한 종합 과정",
                url = "https://example.com/course/$skill",
                platform = "Udemy",
                estimatedHours = 30,
                cost = if (maxBudget != null && maxBudget > 0) minOf(50000, maxBudget) else 50000,
                difficulty = level ?: "INTERMEDIATE",
                isFree = false,
                language = "ko"
            ),
            LearningResource(
                type = ResourceType.DOCUMENTATION,
                title = "$skill 공식 문서",
                description = "공식 문서와 튜토리얼",
                url = "https://docs.example.com/$skill",
                platform = "Official Docs",
                estimatedHours = 20,
                cost = null,
                difficulty = "ALL",
                isFree = true,
                language = "en"
            ),
            LearningResource(
                type = ResourceType.BOOK,
                title = "$skill 인 액션",
                description = "실전 예제로 배우는 $skill",
                url = null,
                platform = "Yes24",
                estimatedHours = 40,
                cost = 35000,
                difficulty = level ?: "ADVANCED",
                isFree = false,
                language = "ko"
            )
        )
        
        return ApiResponse.success(resources)
    }
}