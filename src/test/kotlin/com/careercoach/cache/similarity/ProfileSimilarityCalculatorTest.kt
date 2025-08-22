package com.careercoach.cache.similarity

import com.careercoach.domain.profile.entity.Experience
import com.careercoach.domain.profile.entity.Profile
import com.careercoach.domain.profile.entity.TechnicalSkill
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class ProfileSimilarityCalculatorTest {
    
    private lateinit var calculator: ProfileSimilarityCalculator
    
    @BeforeEach
    fun setup() {
        calculator = ProfileSimilarityCalculator()
    }
    
    @Test
    fun `동일한 프로필은 100% 유사도를 가진다`() {
        // Given
        val profile = createMockProfile(
            yearsOfExperience = 5,
            currentPosition = "백엔드 개발자"
        )
        val skills = listOf(
            createMockSkill("Spring Boot", TechnicalSkill.SkillLevel.ADVANCED, 3),
            createMockSkill("Java", TechnicalSkill.SkillLevel.ADVANCED, 5)
        )
        
        // When
        val similarity = calculator.calculateSimilarity(
            profile, skills,
            profile, skills,
            "백엔드 개발자", "네이버"
        )
        
        // Then
        assertEquals(1.0, similarity.overallScore, 0.01)
        assertTrue(similarity.isHighlySimilar)
        assertTrue(similarity.isCacheable)
    }
    
    @Test
    fun `경력 차이가 클수록 유사도가 낮아진다`() {
        // Given
        val profile1 = createMockProfile(yearsOfExperience = 2)
        val profile2 = createMockProfile(yearsOfExperience = 10)
        val skills = emptyList<TechnicalSkill>()
        
        // When
        val similarity = calculator.calculateSimilarity(
            profile1, skills,
            profile2, skills
        )
        
        // Then
        assertTrue(similarity.experienceSimilarity < 0.5)
    }
    
    @Test
    fun `같은 기술 스택을 가진 프로필은 높은 스킬 유사도를 가진다`() {
        // Given
        val profile1 = createMockProfile()
        val profile2 = createMockProfile()
        
        val skills1 = listOf(
            createMockSkill("Java", TechnicalSkill.SkillLevel.ADVANCED, 5),
            createMockSkill("Spring", TechnicalSkill.SkillLevel.INTERMEDIATE, 3),
            createMockSkill("MySQL", TechnicalSkill.SkillLevel.INTERMEDIATE, 3)
        )
        
        val skills2 = listOf(
            createMockSkill("Java", TechnicalSkill.SkillLevel.ADVANCED, 4),
            createMockSkill("Spring", TechnicalSkill.SkillLevel.ADVANCED, 4),
            createMockSkill("MySQL", TechnicalSkill.SkillLevel.INTERMEDIATE, 2)
        )
        
        // When
        val similarity = calculator.calculateSimilarity(
            profile1, skills1,
            profile2, skills2
        )
        
        // Then
        assertTrue(similarity.skillsSimilarity > 0.7)
    }
    
    @Test
    fun `포지션 키워드가 일치하면 높은 포지션 유사도를 가진다`() {
        // Given
        val profile1 = createMockProfile(currentPosition = "시니어 백엔드 개발자")
        val profile2 = createMockProfile(currentPosition = "백엔드 엔지니어")
        
        // When
        val similarity = calculator.calculateSimilarity(
            profile1, emptyList(),
            profile2, emptyList(),
            targetPosition = "백엔드 개발자"
        )
        
        // Then
        assertTrue(similarity.positionSimilarity > 0.6)
    }
    
    @Test
    fun `같은 회사 타입은 높은 회사 유사도를 가진다`() {
        // Given
        val profile1 = createMockProfile()
        val profile2 = createMockProfile()
        
        val experiences1 = listOf(
            createMockExperience("네이버"),
            createMockExperience("카카오")
        )
        val experiences2 = listOf(
            createMockExperience("쿠팡"),
            createMockExperience("토스")
        )
        
        every { profile1.experiences } returns experiences1.toMutableList()
        every { profile2.experiences } returns experiences2.toMutableList()
        
        // When
        val similarity = calculator.calculateSimilarity(
            profile1, emptyList(),
            profile2, emptyList()
        )
        
        // Then
        assertTrue(similarity.companyTypeSimilarity > 0.5) // 모두 빅테크 회사
    }
    
    @Test
    fun `유사도가 임계값 이상이면 캐싱 가능하다`() {
        // Given
        val profile1 = createMockProfile(
            yearsOfExperience = 5,
            currentPosition = "백엔드 개발자"
        )
        val profile2 = createMockProfile(
            yearsOfExperience = 4,
            currentPosition = "서버 개발자"
        )
        
        val skills = listOf(
            createMockSkill("Java", TechnicalSkill.SkillLevel.ADVANCED, 4),
            createMockSkill("Spring", TechnicalSkill.SkillLevel.ADVANCED, 3)
        )
        
        // When
        val similarity = calculator.calculateSimilarity(
            profile1, skills,
            profile2, skills
        )
        
        // Then
        assertTrue(similarity.overallScore >= ProfileSimilarityCalculator.SIMILARITY_THRESHOLD)
        assertTrue(similarity.isCacheable)
    }
    
    private fun createMockProfile(
        yearsOfExperience: Int = 5,
        currentPosition: String? = "개발자"
    ): Profile {
        return mockk {
            every { this@mockk.yearsOfExperience } returns yearsOfExperience
            every { this@mockk.currentPosition } returns currentPosition
            every { experiences } returns mutableListOf()
            every { name } returns "Test User"
        }
    }
    
    private fun createMockSkill(
        name: String,
        level: TechnicalSkill.SkillLevel,
        years: Int
    ): TechnicalSkill {
        return mockk {
            every { this@mockk.name } returns name
            every { this@mockk.level } returns level
            every { yearsOfExperience } returns years
            every { category } returns "Backend"
        }
    }
    
    private fun createMockExperience(company: String): Experience {
        return mockk {
            every { this@mockk.company } returns company
            every { position } returns "개발자"
            every { startDate } returns LocalDate.now().minusYears(2)
            every { endDate } returns LocalDate.now()
        }
    }
}