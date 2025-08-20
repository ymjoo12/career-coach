package com.careercoach.domain.interview.entity

import com.careercoach.common.entity.BaseEntity
import com.careercoach.domain.profile.entity.Profile
import jakarta.persistence.*

@Entity
@Table(
    name = "interview_question_sets",
    indexes = [
        Index(name = "idx_question_sets_profile_id", columnList = "profile_id")
    ]
)
class InterviewQuestionSet(
    @Column(name = "target_position", length = 200)
    var targetPosition: String? = null,
    
    @Column(name = "target_company", length = 200)
    var targetCompany: String? = null,
    
    @Column(name = "generation_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var generationType: GenerationType = GenerationType.BASIC,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    var profile: Profile? = null,
    
    @OneToMany(mappedBy = "questionSet", cascade = [CascadeType.ALL], orphanRemoval = true)
    val questions: MutableList<InterviewQuestion> = mutableListOf()
) : BaseEntity() {
    
    enum class GenerationType {
        BASIC,          // 기본 질문
        POSITION,       // 포지션 특화
        COMPANY,        // 회사 특화
        COMPREHENSIVE   // 종합
    }
    
    fun addQuestion(question: InterviewQuestion) {
        questions.add(question)
        question.questionSet = this
    }
    
    fun removeQuestion(question: InterviewQuestion) {
        questions.remove(question)
        question.questionSet = null
    }
    
    fun getQuestionsByCategory(category: String): List<InterviewQuestion> {
        return questions.filter { it.category == category }
    }
    
    fun getQuestionsByDifficulty(difficulty: InterviewQuestion.Difficulty): List<InterviewQuestion> {
        return questions.filter { it.difficulty == difficulty }
    }
}