package com.careercoach.domain.interview.repository

import com.careercoach.domain.interview.entity.InterviewQuestionSet
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface InterviewQuestionSetRepository : JpaRepository<InterviewQuestionSet, Long> {
    
    fun findByProfileId(profileId: Long, pageable: Pageable): Page<InterviewQuestionSet>
    
    @Query("""
        SELECT qs FROM InterviewQuestionSet qs
        LEFT JOIN FETCH qs.questions
        WHERE qs.id = :id
    """)
    fun findByIdWithQuestions(@Param("id") id: Long): Optional<InterviewQuestionSet>
    
    fun findByProfileIdAndTargetPosition(
        profileId: Long,
        targetPosition: String
    ): List<InterviewQuestionSet>
    
    fun countByProfileId(profileId: Long): Long
}