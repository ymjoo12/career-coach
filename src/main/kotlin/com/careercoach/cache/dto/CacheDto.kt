package com.careercoach.cache.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "캐시 통계 응답")
data class CacheStatisticsResponse(
    @Schema(description = "캐시 히트율", example = "75.50%")
    val hitRate: String,
    
    @Schema(description = "총 캐시 히트 수", example = "1234")
    val totalHits: Long,
    
    @Schema(description = "총 캐시 미스 수", example = "456")
    val totalMisses: Long,
    
    @Schema(description = "평균 히트 응답 시간", example = "12.34 ms")
    val averageHitTime: String,
    
    @Schema(description = "평균 미스 응답 시간", example = "234.56 ms")
    val averageMissTime: String,
    
    @Schema(description = "캐시 크기 정보")
    val cacheSize: Map<String, Int>,
    
    @Schema(description = "총 캐시 엔트리 수", example = "789")
    val totalCacheEntries: Int
)

@Schema(description = "유사도 계산 요청")
data class SimilarityCalculationRequest(
    @field:NotNull
    @field:Positive
    @Schema(description = "첫 번째 프로필 ID", example = "1")
    val profileId1: Long,
    
    @field:NotNull
    @field:Positive
    @Schema(description = "두 번째 프로필 ID", example = "2")
    val profileId2: Long,
    
    @Schema(description = "목표 포지션", example = "시니어 백엔드 개발자")
    val targetPosition: String? = null,
    
    @Schema(description = "목표 회사", example = "네이버")
    val targetCompany: String? = null
)

@Schema(description = "유사도 응답")
data class SimilarityResponse(
    @Schema(description = "첫 번째 프로필 ID")
    val profileId1: Long,
    
    @Schema(description = "두 번째 프로필 ID")
    val profileId2: Long,
    
    @Schema(description = "전체 유사도 점수", example = "0.85")
    val overallScore: Double,
    
    @Schema(description = "경력 유사도", example = "0.90")
    val experienceSimilarity: Double,
    
    @Schema(description = "포지션 유사도", example = "0.80")
    val positionSimilarity: Double,
    
    @Schema(description = "기술 스택 유사도", example = "0.85")
    val skillsSimilarity: Double,
    
    @Schema(description = "회사 유형 유사도", example = "0.75")
    val companyTypeSimilarity: Double,
    
    @Schema(description = "학력 유사도", example = "0.70")
    val educationSimilarity: Double,
    
    @Schema(description = "높은 유사도 여부")
    val isHighlySimilar: Boolean,
    
    @Schema(description = "캐시 가능 여부")
    val isCacheable: Boolean,
    
    @Schema(description = "유사도 설명")
    val details: String,
    
    @Schema(description = "추천 사항")
    val recommendation: String
)

@Schema(description = "캐시된 데이터 응답")
data class CachedDataResponse(
    @Schema(description = "캐시 데이터 발견 여부")
    val found: Boolean,
    
    @Schema(description = "캐시 타입", example = "questions")
    val type: String,
    
    @Schema(description = "캐시된 데이터")
    val data: String? = null,
    
    @Schema(description = "신뢰도", example = "0.85")
    val confidence: Double? = null,
    
    @Schema(description = "유사도 점수", example = "0.90")
    val similarityScore: Double? = null,
    
    @Schema(description = "메타데이터")
    val metadata: Map<String, Any>? = null,
    
    @Schema(description = "메시지")
    val message: String
)

@Schema(description = "캐시 워밍업 요청")
data class CacheWarmupRequest(
    @Schema(description = "워밍업할 프로필 ID 목록")
    val profileIds: List<Long> = emptyList(),
    
    @Schema(description = "프로필 수 제한", example = "10")
    val limit: Int = 10,
    
    @Schema(description = "대상 포지션 목록")
    val targetPositions: List<String> = emptyList(),
    
    @Schema(description = "대상 회사 목록")
    val targetCompanies: List<String> = emptyList()
)

@Schema(description = "캐시 워밍업 응답")
data class CacheWarmupResponse(
    @Schema(description = "처리된 프로필 수")
    val profilesProcessed: Int,
    
    @Schema(description = "생성된 캐시 엔트리 수")
    val cacheEntriesCreated: Int,
    
    @Schema(description = "메시지")
    val message: String
)

@Schema(description = "캐시 엔트리 정보")
data class CacheEntryInfo(
    @Schema(description = "캐시 키")
    val key: String,
    
    @Schema(description = "프로필 ID")
    val profileId: Long,
    
    @Schema(description = "목표 포지션")
    val targetPosition: String?,
    
    @Schema(description = "목표 회사")
    val targetCompany: String?,
    
    @Schema(description = "캐시 생성 시간")
    val cachedAt: String,
    
    @Schema(description = "캐시 타입")
    val type: String,
    
    @Schema(description = "데이터 크기 (bytes)")
    val dataSize: Int
)