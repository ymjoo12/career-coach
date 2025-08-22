package com.careercoach.cache.similarity

import com.careercoach.domain.profile.entity.Profile
import com.careercoach.domain.profile.entity.TechnicalSkill
import org.springframework.stereotype.Component
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Component
class ProfileSimilarityCalculator {
    
    companion object {
        // Weight factors for different aspects
        const val WEIGHT_EXPERIENCE_YEARS = 0.25
        const val WEIGHT_POSITION = 0.20
        const val WEIGHT_SKILLS = 0.35
        const val WEIGHT_COMPANY_TYPE = 0.10
        const val WEIGHT_EDUCATION = 0.10
        
        // Similarity threshold for caching
        const val SIMILARITY_THRESHOLD = 0.80
        const val HIGH_SIMILARITY_THRESHOLD = 0.90
    }
    
    fun calculateSimilarity(
        profile1: Profile,
        skills1: List<TechnicalSkill>,
        profile2: Profile,
        skills2: List<TechnicalSkill>,
        targetPosition: String? = null,
        targetCompany: String? = null
    ): SimilarityScore {
        
        val experienceSimilarity = calculateExperienceSimilarity(
            profile1.yearsOfExperience,
            profile2.yearsOfExperience
        )
        
        val positionSimilarity = calculatePositionSimilarity(
            profile1.currentPosition,
            profile2.currentPosition,
            targetPosition
        )
        
        val skillsSimilarity = calculateSkillsSimilarity(skills1, skills2)
        
        val companyTypeSimilarity = calculateCompanyTypeSimilarity(
            profile1.experiences,
            profile2.experiences,
            targetCompany
        )
        
        // Education similarity (profile doesn't have educationLevel yet, using default)
        val educationSimilarity = 0.7 // Default similarity for now
        
        val overallScore = (
            experienceSimilarity * WEIGHT_EXPERIENCE_YEARS +
            positionSimilarity * WEIGHT_POSITION +
            skillsSimilarity * WEIGHT_SKILLS +
            companyTypeSimilarity * WEIGHT_COMPANY_TYPE +
            educationSimilarity * WEIGHT_EDUCATION
        )
        
        return SimilarityScore(
            overallScore = overallScore,
            experienceSimilarity = experienceSimilarity,
            positionSimilarity = positionSimilarity,
            skillsSimilarity = skillsSimilarity,
            companyTypeSimilarity = companyTypeSimilarity,
            educationSimilarity = educationSimilarity,
            isHighlySimilar = overallScore >= HIGH_SIMILARITY_THRESHOLD,
            isCacheable = overallScore >= SIMILARITY_THRESHOLD,
            details = generateDetails(overallScore)
        )
    }
    
    private fun calculateExperienceSimilarity(years1: Int, years2: Int): Double {
        val diff = abs(years1 - years2)
        return when {
            diff == 0 -> 1.0
            diff <= 1 -> 0.9
            diff <= 2 -> 0.8
            diff <= 3 -> 0.6
            diff <= 5 -> 0.4
            else -> max(0.0, 1.0 - diff * 0.1)
        }
    }
    
    private fun calculatePositionSimilarity(
        position1: String?,
        position2: String?,
        targetPosition: String?
    ): Double {
        if (position1 == null || position2 == null) return 0.5
        
        val pos1Lower = position1.lowercase()
        val pos2Lower = position2.lowercase()
        
        // Exact match
        if (pos1Lower == pos2Lower) return 1.0
        
        // Check for common keywords
        val keywords = extractPositionKeywords(pos1Lower)
        val keywords2 = extractPositionKeywords(pos2Lower)
        
        if (keywords.isEmpty() || keywords2.isEmpty()) return 0.3
        
        val intersection = keywords.intersect(keywords2).size
        val union = keywords.union(keywords2).size
        
        val jaccardSimilarity = if (union > 0) intersection.toDouble() / union else 0.0
        
        // Boost if both match target position
        val targetBoost = if (targetPosition != null) {
            val targetKeywords = extractPositionKeywords(targetPosition.lowercase())
            val match1 = keywords.intersect(targetKeywords).isNotEmpty()
            val match2 = keywords2.intersect(targetKeywords).isNotEmpty()
            if (match1 && match2) 0.2 else 0.0
        } else 0.0
        
        return min(1.0, jaccardSimilarity + targetBoost)
    }
    
    private fun extractPositionKeywords(position: String): Set<String> {
        val keywords = mutableSetOf<String>()
        
        // Common position keywords
        val positionTerms = listOf(
            "backend", "frontend", "fullstack", "devops", "데브옵스",
            "senior", "junior", "lead", "principal", "staff",
            "시니어", "주니어", "리드", "팀장",
            "developer", "engineer", "architect", "manager",
            "개발자", "엔지니어", "아키텍트", "매니저",
            "mobile", "ios", "android", "web", "cloud",
            "모바일", "웹", "클라우드",
            "data", "ml", "ai", "데이터", "머신러닝"
        )
        
        positionTerms.forEach { term ->
            if (position.contains(term)) {
                keywords.add(term)
            }
        }
        
        return keywords
    }
    
    private fun calculateSkillsSimilarity(
        skills1: List<TechnicalSkill>,
        skills2: List<TechnicalSkill>
    ): Double {
        if (skills1.isEmpty() && skills2.isEmpty()) return 1.0
        if (skills1.isEmpty() || skills2.isEmpty()) return 0.0
        
        val skillMap1 = skills1.associateBy { it.name.lowercase() }
        val skillMap2 = skills2.associateBy { it.name.lowercase() }
        
        val allSkills = skillMap1.keys.union(skillMap2.keys)
        var totalSimilarity = 0.0
        
        for (skill in allSkills) {
            val skill1 = skillMap1[skill]
            val skill2 = skillMap2[skill]
            
            when {
                skill1 != null && skill2 != null -> {
                    // Both have the skill - compare levels
                    val levelSim = calculateLevelSimilarity(skill1.level, skill2.level)
                    val yearsSim = calculateYearsSimilarity(
                        skill1.yearsOfExperience ?: 0,
                        skill2.yearsOfExperience ?: 0
                    )
                    totalSimilarity += (levelSim * 0.6 + yearsSim * 0.4)
                }
                skill1 != null || skill2 != null -> {
                    // Only one has the skill - partial credit for related skills
                    totalSimilarity += if (areSkillsRelated(skill, allSkills)) 0.3 else 0.1
                }
            }
        }
        
        return totalSimilarity / allSkills.size
    }
    
    private fun calculateLevelSimilarity(level1: TechnicalSkill.SkillLevel?, level2: TechnicalSkill.SkillLevel?): Double {
        if (level1 == null || level2 == null) return 0.5
        
        val levels = listOf(
            TechnicalSkill.SkillLevel.BEGINNER,
            TechnicalSkill.SkillLevel.INTERMEDIATE,
            TechnicalSkill.SkillLevel.ADVANCED,
            TechnicalSkill.SkillLevel.EXPERT
        )
        
        val index1 = levels.indexOf(level1)
        val index2 = levels.indexOf(level2)
        val diff = abs(index1 - index2)
        
        return when (diff) {
            0 -> 1.0
            1 -> 0.7
            2 -> 0.4
            else -> 0.1
        }
    }
    
    private fun calculateYearsSimilarity(years1: Int, years2: Int): Double {
        val diff = abs(years1 - years2)
        return when {
            diff == 0 -> 1.0
            diff <= 1 -> 0.8
            diff <= 2 -> 0.6
            else -> max(0.0, 1.0 - diff * 0.2)
        }
    }
    
    private fun areSkillsRelated(skill: String, allSkills: Set<String>): Boolean {
        val relatedGroups = listOf(
            setOf("java", "kotlin", "spring", "springboot"),
            setOf("javascript", "typescript", "react", "vue", "angular", "node", "nodejs"),
            setOf("python", "django", "flask", "fastapi"),
            setOf("aws", "gcp", "azure", "cloud"),
            setOf("docker", "kubernetes", "k8s"),
            setOf("mysql", "postgresql", "oracle", "mariadb"),
            setOf("mongodb", "redis", "elasticsearch", "cassandra"),
            setOf("jenkins", "gitlab", "github", "ci/cd", "devops")
        )
        
        for (group in relatedGroups) {
            if (group.contains(skill)) {
                return group.intersect(allSkills).size > 1
            }
        }
        
        return false
    }
    
    private fun calculateCompanyTypeSimilarity(
        experiences1: List<com.careercoach.domain.profile.entity.Experience>,
        experiences2: List<com.careercoach.domain.profile.entity.Experience>,
        targetCompany: String?
    ): Double {
        if (experiences1.isEmpty() && experiences2.isEmpty()) return 1.0
        
        val companies1 = experiences1.map { it.company.lowercase() }.toSet()
        val companies2 = experiences2.map { it.company.lowercase() }.toSet()
        
        // Check for exact company matches
        val commonCompanies = companies1.intersect(companies2)
        if (commonCompanies.isNotEmpty()) return 1.0
        
        // Check for company type similarity
        val types1 = extractCompanyTypes(companies1)
        val types2 = extractCompanyTypes(companies2)
        
        if (types1.isEmpty() || types2.isEmpty()) return 0.5
        
        val typeIntersection = types1.intersect(types2).size
        val typeUnion = types1.union(types2).size
        
        return if (typeUnion > 0) typeIntersection.toDouble() / typeUnion else 0.5
    }
    
    private fun extractCompanyTypes(companies: Set<String>): Set<String> {
        val types = mutableSetOf<String>()
        
        val typePatterns = mapOf(
            "startup" to listOf("스타트업", "startup", "labs", "ventures"),
            "bigtech" to listOf("네이버", "카카오", "쿠팡", "배달의민족", "토스", "당근", "naver", "kakao", "coupang", "toss"),
            "gaming" to listOf("게임", "game", "넥슨", "넷마블", "nexon", "netmarble"),
            "finance" to listOf("은행", "bank", "금융", "finance", "카드", "card", "보험", "insurance"),
            "ecommerce" to listOf("커머스", "commerce", "쇼핑", "shopping", "마켓", "market")
        )
        
        for (company in companies) {
            for ((type, patterns) in typePatterns) {
                if (patterns.any { company.contains(it) }) {
                    types.add(type)
                }
            }
        }
        
        return types
    }
    
    private fun calculateEducationSimilarity(edu1: String?, edu2: String?): Double {
        if (edu1 == null && edu2 == null) return 1.0
        if (edu1 == null || edu2 == null) return 0.5
        
        val levels = mapOf(
            "high_school" to 1,
            "associate" to 2,
            "bachelor" to 3,
            "master" to 4,
            "phd" to 5
        )
        
        val level1 = levels[edu1.lowercase()] ?: 3
        val level2 = levels[edu2.lowercase()] ?: 3
        val diff = abs(level1 - level2)
        
        return when (diff) {
            0 -> 1.0
            1 -> 0.7
            2 -> 0.4
            else -> 0.1
        }
    }
    
    private fun generateDetails(score: Double): String {
        return when {
            score >= 0.9 -> "매우 유사한 프로필 (90% 이상)"
            score >= 0.8 -> "높은 유사도 프로필 (80% 이상)"
            score >= 0.7 -> "중간 유사도 프로필 (70% 이상)"
            score >= 0.6 -> "일부 유사한 프로필 (60% 이상)"
            else -> "낮은 유사도 프로필"
        }
    }
}

data class SimilarityScore(
    val overallScore: Double,
    val experienceSimilarity: Double,
    val positionSimilarity: Double,
    val skillsSimilarity: Double,
    val companyTypeSimilarity: Double,
    val educationSimilarity: Double,
    val isHighlySimilar: Boolean,
    val isCacheable: Boolean,
    val details: String
)