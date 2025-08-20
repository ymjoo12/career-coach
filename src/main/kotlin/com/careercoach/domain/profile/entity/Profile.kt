package com.careercoach.domain.profile.entity

import com.careercoach.common.entity.BaseEntity
import com.careercoach.domain.interview.entity.InterviewQuestionSet
import com.careercoach.domain.learning.entity.LearningRoadmap
import jakarta.persistence.*

@Entity
@Table(
    name = "profiles",
    indexes = [
        Index(name = "idx_profiles_email", columnList = "email")
    ]
)
class Profile(
    @Column(nullable = false, length = 100)
    var name: String,
    
    @Column(nullable = false, unique = true)
    var email: String,
    
    @Column(length = 20)
    var phone: String? = null,
    
    @Column(columnDefinition = "TEXT")
    var summary: String? = null,
    
    @Column(name = "years_of_experience")
    var yearsOfExperience: Int = 0,
    
    @OneToMany(mappedBy = "profile", cascade = [CascadeType.ALL], orphanRemoval = true)
    val experiences: MutableList<Experience> = mutableListOf(),
    
    @OneToMany(mappedBy = "profile", cascade = [CascadeType.ALL], orphanRemoval = true)
    val technicalSkills: MutableList<TechnicalSkill> = mutableListOf(),
    
    @OneToMany(mappedBy = "profile", cascade = [CascadeType.REMOVE])
    val interviewQuestionSets: MutableList<InterviewQuestionSet> = mutableListOf(),
    
    @OneToMany(mappedBy = "profile", cascade = [CascadeType.REMOVE])
    val learningRoadmaps: MutableList<LearningRoadmap> = mutableListOf()
) : BaseEntity() {
    
    fun addExperience(experience: Experience) {
        experiences.add(experience)
        experience.profile = this
    }
    
    fun removeExperience(experience: Experience) {
        experiences.remove(experience)
        experience.profile = null
    }
    
    fun addTechnicalSkill(skill: TechnicalSkill) {
        technicalSkills.add(skill)
        skill.profile = this
    }
    
    fun removeTechnicalSkill(skill: TechnicalSkill) {
        technicalSkills.remove(skill)
        skill.profile = null
    }
    
    fun calculateTotalExperience(): Int {
        return experiences.sumOf { it.calculateDuration() }
    }
}