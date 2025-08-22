package com.careercoach.cache.controller

import com.careercoach.cache.dto.*
import com.careercoach.cache.service.SimilarityBasedCacheService
import com.careercoach.cache.similarity.ProfileSimilarityCalculator
import com.careercoach.common.dto.ApiResponse
import com.careercoach.domain.profile.repository.ProfileRepository
import com.careercoach.domain.profile.repository.TechnicalSkillRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/cache")
@Tag(name = "Cache Management", description = "캐시 관리 API")
class CacheController(
    private val cacheService: SimilarityBasedCacheService,
    private val similarityCalculator: ProfileSimilarityCalculator,
    private val profileRepository: ProfileRepository,
    private val technicalSkillRepository: TechnicalSkillRepository
) {
    
    @GetMapping("/statistics")
    @Operation(summary = "캐시 통계 조회", description = "캐시 히트율, 응답 시간 등 통계 정보 조회")
    fun getCacheStatistics(): ApiResponse<CacheStatisticsResponse> {
        val stats = cacheService.getCacheStatistics()
        val sizeInfo = cacheService.getCacheSizeInfo()
        
        return ApiResponse.success(
            CacheStatisticsResponse(
                hitRate = String.format("%.2f%%", stats.getHitRate() * 100),
                totalHits = stats.totalHits,
                totalMisses = stats.totalMisses,
                averageHitTime = String.format("%.2f ms", stats.getAverageHitTime()),
                averageMissTime = String.format("%.2f ms", stats.getAverageMissTime()),
                cacheSize = sizeInfo,
                totalCacheEntries = sizeInfo.values.sum()
            )
        )
    }
    
    @PostMapping("/similarity/calculate")
    @Operation(summary = "프로필 유사도 계산", description = "두 프로필 간의 유사도를 계산")
    fun calculateSimilarity(
        @Valid @RequestBody request: SimilarityCalculationRequest
    ): ApiResponse<SimilarityResponse> {
        val profile1 = profileRepository.findById(request.profileId1)
            .orElseThrow { IllegalArgumentException("Profile 1 not found") }
        val skills1 = technicalSkillRepository.findByProfileId(request.profileId1)
        
        val profile2 = profileRepository.findById(request.profileId2)
            .orElseThrow { IllegalArgumentException("Profile 2 not found") }
        val skills2 = technicalSkillRepository.findByProfileId(request.profileId2)
        
        val similarity = similarityCalculator.calculateSimilarity(
            profile1, skills1,
            profile2, skills2,
            request.targetPosition,
            request.targetCompany
        )
        
        return ApiResponse.success(
            SimilarityResponse(
                profileId1 = request.profileId1,
                profileId2 = request.profileId2,
                overallScore = similarity.overallScore,
                experienceSimilarity = similarity.experienceSimilarity,
                positionSimilarity = similarity.positionSimilarity,
                skillsSimilarity = similarity.skillsSimilarity,
                companyTypeSimilarity = similarity.companyTypeSimilarity,
                educationSimilarity = similarity.educationSimilarity,
                isHighlySimilar = similarity.isHighlySimilar,
                isCacheable = similarity.isCacheable,
                details = similarity.details,
                recommendation = generateRecommendation(similarity)
            )
        )
    }
    
    @GetMapping("/profile/{profileId}/similar-cached")
    @Operation(summary = "유사 캐시 조회", description = "프로필과 유사한 캐시된 데이터 조회")
    fun findSimilarCached(
        @PathVariable profileId: Long,
        @RequestParam(required = false) targetPosition: String?,
        @RequestParam(required = false) targetCompany: String?,
        @RequestParam(defaultValue = "questions") type: String
    ): ApiResponse<CachedDataResponse> {
        
        val cachedData = when (type) {
            "questions" -> {
                val cached = cacheService.findSimilarCachedQuestions(profileId, targetPosition, targetCompany)
                if (cached != null) {
                    CachedDataResponse(
                        found = true,
                        type = "questions",
                        data = cached.questions,
                        confidence = cached.confidence,
                        similarityScore = cached.similarityScore,
                        metadata = cached.metadata,
                        message = "유사한 프로필의 캐시된 질문을 찾았습니다."
                    )
                } else {
                    CachedDataResponse(
                        found = false,
                        type = "questions",
                        message = "유사한 캐시 데이터를 찾을 수 없습니다."
                    )
                }
            }
            "learning-path" -> {
                val cached = cacheService.findSimilarCachedLearningPath(profileId, targetPosition, targetCompany)
                if (cached != null) {
                    CachedDataResponse(
                        found = true,
                        type = "learning-path",
                        data = cached.learningPath,
                        confidence = cached.confidence,
                        similarityScore = cached.similarityScore,
                        metadata = cached.metadata,
                        message = "유사한 프로필의 캐시된 학습 경로를 찾았습니다."
                    )
                } else {
                    CachedDataResponse(
                        found = false,
                        type = "learning-path",
                        message = "유사한 캐시 데이터를 찾을 수 없습니다."
                    )
                }
            }
            else -> {
                CachedDataResponse(
                    found = false,
                    type = type,
                    message = "지원하지 않는 캐시 타입입니다."
                )
            }
        }
        
        return ApiResponse.success(cachedData)
    }
    
    @DeleteMapping("/clear")
    @Operation(summary = "캐시 초기화", description = "모든 캐시 데이터 삭제")
    fun clearCache(): ApiResponse<String> {
        cacheService.clearAllCaches()
        return ApiResponse.success("All caches cleared successfully")
    }
    
    @PostMapping("/warmup")
    @Operation(summary = "캐시 워밍업", description = "자주 사용되는 프로필 조합에 대한 캐시 사전 생성")
    fun warmupCache(@Valid @RequestBody request: CacheWarmupRequest): ApiResponse<CacheWarmupResponse> {
        val profiles = if (request.profileIds.isNotEmpty()) {
            profileRepository.findAllById(request.profileIds)
        } else {
            // Get top N active profiles
            profileRepository.findAll().take(request.limit)
        }
        
        var warmedCount = 0
        val commonPositions = listOf("백엔드 개발자", "프론트엔드 개발자", "풀스택 개발자", "데이터 엔지니어")
        val commonCompanies = listOf("네이버", "카카오", "쿠팡", "토스", null)
        
        for (profile in profiles) {
            for (position in commonPositions) {
                for (company in commonCompanies) {
                    // Simulate cache population
                    val mockQuestions = """
                        {
                            "questions": [
                                {
                                    "type": "warmup",
                                    "question": "Cached for ${profile.name} - $position at $company"
                                }
                            ]
                        }
                    """.trimIndent()
                    
                    cacheService.cacheQuestions(
                        profile.id!!,
                        position,
                        company,
                        mockQuestions,
                        mapOf("warmup" to true)
                    )
                    warmedCount++
                }
            }
        }
        
        return ApiResponse.success(
            CacheWarmupResponse(
                profilesProcessed = profiles.size,
                cacheEntriesCreated = warmedCount,
                message = "Cache warmup completed successfully"
            )
        )
    }
    
    private fun generateRecommendation(similarity: com.careercoach.cache.similarity.SimilarityScore): String {
        return when {
            similarity.isHighlySimilar -> 
                "프로필이 매우 유사합니다. 캐시된 결과를 그대로 사용해도 좋습니다."
            similarity.isCacheable -> 
                "프로필이 충분히 유사합니다. 캐시된 결과를 참고하되 일부 조정이 필요할 수 있습니다."
            similarity.overallScore >= 0.6 -> 
                "중간 정도의 유사도입니다. 캐시를 참고용으로만 사용하세요."
            else -> 
                "유사도가 낮습니다. 새로운 결과를 생성하는 것을 권장합니다."
        }
    }
}