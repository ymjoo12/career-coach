package com.careercoach.domain.profile.entity

import com.careercoach.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "technical_skills",
    indexes = [
        Index(name = "idx_skills_profile_id", columnList = "profile_id")
    ]
)
class TechnicalSkill(
    @Column(nullable = false, length = 100)
    var category: String,
    
    @Column(nullable = false, length = 100)
    var name: String,
    
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    var level: SkillLevel? = SkillLevel.INTERMEDIATE,
    
    @Column(name = "years_of_experience")
    var yearsOfExperience: Int = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    var profile: Profile? = null
) : BaseEntity() {
    
    enum class SkillLevel {
        BEGINNER,       // 초급
        INTERMEDIATE,   // 중급
        ADVANCED,       // 고급
        EXPERT         // 전문가
    }
    
    fun getLevelDescription(): String {
        return when (level) {
            SkillLevel.BEGINNER -> "초급"
            SkillLevel.INTERMEDIATE -> "중급"
            SkillLevel.ADVANCED -> "고급"
            SkillLevel.EXPERT -> "전문가"
            null -> "미지정"
        }
    }
}