package com.careercoach.cache.service

import com.careercoach.cache.similarity.ProfileSimilarityCalculator
import com.careercoach.cache.similarity.SimilarityScore
import com.careercoach.domain.profile.entity.Profile
import com.careercoach.domain.profile.entity.TechnicalSkill
import com.careercoach.domain.profile.repository.ProfileRepository
import com.careercoach.domain.profile.repository.TechnicalSkillRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class SimilarityBasedCacheService(
    private val profileRepository: ProfileRepository,
    private val technicalSkillRepository: TechnicalSkillRepository,
    private val similarityCalculator: ProfileSimilarityCalculator
) {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    // Cache storage
    private val questionCache = ConcurrentHashMap<CacheKey, CachedQuestions>()
    private val learningPathCache = ConcurrentHashMap<CacheKey, CachedLearningPath>()
    private val similarityCache = ConcurrentHashMap<ProfilePair, SimilarityScore>()
    
    // Cache statistics
    private val cacheStats = CacheStatistics()
    
    fun findSimilarCachedQuestions(
        profileId: Long,
        targetPosition: String?,
        targetCompany: String?
    ): CachedQuestions? {
        val startTime = System.currentTimeMillis()
        
        val profile = profileRepository.findById(profileId).orElse(null) ?: return null
        val skills = technicalSkillRepository.findByProfileId(profileId)
        
        // Search for similar profiles in cache
        for ((cacheKey, cachedQuestions) in questionCache) {
            if (isCacheValid(cachedQuestions.cachedAt)) {
                val similarity = calculateOrGetSimilarity(
                    profile, skills,
                    cacheKey.profileId,
                    targetPosition, targetCompany
                )
                
                if (similarity != null && similarity.isCacheable) {
                    // Check if target position/company match
                    val positionMatch = targetPosition == null || 
                        cacheKey.targetPosition == targetPosition ||
                        areSimilarPositions(cacheKey.targetPosition, targetPosition)
                    
                    val companyMatch = targetCompany == null ||
                        cacheKey.targetCompany == targetCompany ||
                        areSimilarCompanies(cacheKey.targetCompany, targetCompany)
                    
                    if (positionMatch && companyMatch) {
                        logger.info("Cache hit for profile $profileId with similarity ${similarity.overallScore}")
                        cacheStats.recordHit(System.currentTimeMillis() - startTime)
                        
                        // Adjust confidence based on similarity
                        return cachedQuestions.copy(
                            confidence = cachedQuestions.confidence * similarity.overallScore,
                            similarityScore = similarity.overallScore
                        )
                    }
                }
            }
        }
        
        cacheStats.recordMiss(System.currentTimeMillis() - startTime)
        return null
    }
    
    fun findSimilarCachedLearningPath(
        profileId: Long,
        targetPosition: String?,
        targetCompany: String?
    ): CachedLearningPath? {
        val startTime = System.currentTimeMillis()
        
        val profile = profileRepository.findById(profileId).orElse(null) ?: return null
        val skills = technicalSkillRepository.findByProfileId(profileId)
        
        for ((cacheKey, cachedPath) in learningPathCache) {
            if (isCacheValid(cachedPath.cachedAt)) {
                val similarity = calculateOrGetSimilarity(
                    profile, skills,
                    cacheKey.profileId,
                    targetPosition, targetCompany
                )
                
                if (similarity != null && similarity.isCacheable) {
                    val positionMatch = targetPosition == null || 
                        cacheKey.targetPosition == targetPosition ||
                        areSimilarPositions(cacheKey.targetPosition, targetPosition)
                    
                    if (positionMatch) {
                        logger.info("Learning path cache hit for profile $profileId")
                        cacheStats.recordHit(System.currentTimeMillis() - startTime)
                        
                        return cachedPath.copy(
                            confidence = cachedPath.confidence * similarity.overallScore,
                            similarityScore = similarity.overallScore
                        )
                    }
                }
            }
        }
        
        cacheStats.recordMiss(System.currentTimeMillis() - startTime)
        return null
    }
    
    fun cacheQuestions(
        profileId: Long,
        targetPosition: String?,
        targetCompany: String?,
        questions: String,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val cacheKey = CacheKey(profileId, targetPosition, targetCompany)
        val cachedQuestions = CachedQuestions(
            questions = questions,
            metadata = metadata,
            cachedAt = LocalDateTime.now(),
            confidence = 1.0,
            similarityScore = 1.0
        )
        
        questionCache[cacheKey] = cachedQuestions
        logger.debug("Cached questions for profile $profileId")
    }
    
    fun cacheLearningPath(
        profileId: Long,
        targetPosition: String?,
        targetCompany: String?,
        learningPath: String,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val cacheKey = CacheKey(profileId, targetPosition, targetCompany)
        val cachedPath = CachedLearningPath(
            learningPath = learningPath,
            metadata = metadata,
            cachedAt = LocalDateTime.now(),
            confidence = 1.0,
            similarityScore = 1.0
        )
        
        learningPathCache[cacheKey] = cachedPath
        logger.debug("Cached learning path for profile $profileId")
    }
    
    private fun calculateOrGetSimilarity(
        profile1: Profile,
        skills1: List<TechnicalSkill>,
        profileId2: Long,
        targetPosition: String?,
        targetCompany: String?
    ): SimilarityScore? {
        val pair = ProfilePair(profile1.id!!, profileId2)
        
        // Check similarity cache first
        similarityCache[pair]?.let { return it }
        
        // Calculate new similarity
        val profile2 = profileRepository.findById(profileId2).orElse(null) ?: return null
        val skills2 = technicalSkillRepository.findByProfileId(profileId2)
        
        val similarity = similarityCalculator.calculateSimilarity(
            profile1, skills1,
            profile2, skills2,
            targetPosition, targetCompany
        )
        
        // Cache the similarity score
        similarityCache[pair] = similarity
        
        return similarity
    }
    
    private fun areSimilarPositions(position1: String?, position2: String?): Boolean {
        if (position1 == null || position2 == null) return false
        
        val normalized1 = normalizePosition(position1)
        val normalized2 = normalizePosition(position2)
        
        return normalized1 == normalized2 || 
               normalized1.contains(normalized2) || 
               normalized2.contains(normalized1)
    }
    
    private fun normalizePosition(position: String): String {
        return position.lowercase()
            .replace("senior", "시니어")
            .replace("junior", "주니어")
            .replace("developer", "개발자")
            .replace("engineer", "엔지니어")
            .replace("backend", "백엔드")
            .replace("frontend", "프론트엔드")
    }
    
    private fun areSimilarCompanies(company1: String?, company2: String?): Boolean {
        if (company1 == null || company2 == null) return false
        
        val normalized1 = company1.lowercase()
        val normalized2 = company2.lowercase()
        
        // Check for exact match or common patterns
        return normalized1 == normalized2 ||
               (isStartup(normalized1) && isStartup(normalized2)) ||
               (isBigTech(normalized1) && isBigTech(normalized2))
    }
    
    private fun isStartup(company: String): Boolean {
        return company.contains("스타트업") || company.contains("startup") || 
               company.contains("labs") || company.contains("ventures")
    }
    
    private fun isBigTech(company: String): Boolean {
        val bigTechCompanies = listOf("네이버", "카카오", "쿠팡", "배달의민족", "토스", "당근마켓")
        return bigTechCompanies.any { company.contains(it) }
    }
    
    private fun isCacheValid(cachedAt: LocalDateTime): Boolean {
        val hoursOld = java.time.Duration.between(cachedAt, LocalDateTime.now()).toHours()
        return hoursOld < 24 // Cache valid for 24 hours
    }
    
    @CacheEvict(value = ["questions", "learningPaths"], allEntries = true)
    fun clearAllCaches() {
        questionCache.clear()
        learningPathCache.clear()
        similarityCache.clear()
        cacheStats.reset()
        logger.info("All caches cleared")
    }
    
    @Scheduled(cron = "0 0 3 * * *") // Run at 3 AM daily
    fun cleanupExpiredCache() {
        val now = LocalDateTime.now()
        var removedCount = 0
        
        questionCache.entries.removeIf { (_, cached) ->
            val shouldRemove = !isCacheValid(cached.cachedAt)
            if (shouldRemove) removedCount++
            shouldRemove
        }
        
        learningPathCache.entries.removeIf { (_, cached) ->
            val shouldRemove = !isCacheValid(cached.cachedAt)
            if (shouldRemove) removedCount++
            shouldRemove
        }
        
        // Clear old similarity scores
        similarityCache.clear()
        
        logger.info("Cache cleanup completed. Removed $removedCount expired entries")
    }
    
    fun getCacheStatistics(): CacheStatistics = cacheStats.copy()
    
    fun getCacheSizeInfo(): Map<String, Int> {
        return mapOf(
            "questionCache" to questionCache.size,
            "learningPathCache" to learningPathCache.size,
            "similarityCache" to similarityCache.size
        )
    }
}

data class CacheKey(
    val profileId: Long,
    val targetPosition: String?,
    val targetCompany: String?
)

data class CachedQuestions(
    val questions: String,
    val metadata: Map<String, Any>,
    val cachedAt: LocalDateTime,
    val confidence: Double,
    val similarityScore: Double
)

data class CachedLearningPath(
    val learningPath: String,
    val metadata: Map<String, Any>,
    val cachedAt: LocalDateTime,
    val confidence: Double,
    val similarityScore: Double
)

data class ProfilePair(
    val profileId1: Long,
    val profileId2: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProfilePair) return false
        
        return (profileId1 == other.profileId1 && profileId2 == other.profileId2) ||
               (profileId1 == other.profileId2 && profileId2 == other.profileId1)
    }
    
    override fun hashCode(): Int {
        return minOf(profileId1, profileId2).hashCode() * 31 + maxOf(profileId1, profileId2).hashCode()
    }
}

data class CacheStatistics(
    var totalHits: Long = 0,
    var totalMisses: Long = 0,
    var totalHitTime: Long = 0,
    var totalMissTime: Long = 0
) {
    fun recordHit(responseTime: Long) {
        totalHits++
        totalHitTime += responseTime
    }
    
    fun recordMiss(responseTime: Long) {
        totalMisses++
        totalMissTime += responseTime
    }
    
    fun getHitRate(): Double {
        val total = totalHits + totalMisses
        return if (total > 0) totalHits.toDouble() / total else 0.0
    }
    
    fun getAverageHitTime(): Double {
        return if (totalHits > 0) totalHitTime.toDouble() / totalHits else 0.0
    }
    
    fun getAverageMissTime(): Double {
        return if (totalMisses > 0) totalMissTime.toDouble() / totalMisses else 0.0
    }
    
    fun reset() {
        totalHits = 0
        totalMisses = 0
        totalHitTime = 0
        totalMissTime = 0
    }
}