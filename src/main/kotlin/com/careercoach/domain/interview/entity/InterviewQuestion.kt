package com.careercoach.domain.interview.entity

import com.careercoach.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "interview_questions",
    indexes = [
        Index(name = "idx_questions_set_id", columnList = "question_set_id")
    ]
)
class InterviewQuestion(
    @Column(nullable = false, columnDefinition = "TEXT")
    var question: String,
    
    @Column(length = 100)
    var category: String? = null,
    
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    var difficulty: Difficulty? = Difficulty.MEDIUM,
    
    @Column(name = "expected_answer_points", columnDefinition = "TEXT")
    var expectedAnswerPoints: String? = null,
    
    @Column(name = "order_index")
    var orderIndex: Int = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_set_id", nullable = false)
    var questionSet: InterviewQuestionSet? = null
) : BaseEntity() {
    
    enum class Difficulty {
        EASY,    // 쉬움
        MEDIUM,  // 보통
        HARD,    // 어려움
        EXPERT   // 전문가
    }
    
    fun getDifficultyDescription(): String {
        return when (difficulty) {
            Difficulty.EASY -> "쉬움"
            Difficulty.MEDIUM -> "보통"
            Difficulty.HARD -> "어려움"
            Difficulty.EXPERT -> "전문가"
            null -> "미지정"
        }
    }
    
    fun getAnswerPointsList(): List<String> {
        return expectedAnswerPoints?.split("\n")?.map { it.trim() } ?: emptyList()
    }
}