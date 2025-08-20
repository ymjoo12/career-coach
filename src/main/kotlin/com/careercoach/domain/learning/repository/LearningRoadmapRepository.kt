package com.careercoach.domain.learning.repository

import com.careercoach.domain.learning.entity.LearningRoadmap
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface LearningRoadmapRepository : JpaRepository<LearningRoadmap, Long> {
    
    fun findByProfileId(profileId: Long, pageable: Pageable): Page<LearningRoadmap>
    
    @Query("""
        SELECT r FROM LearningRoadmap r
        LEFT JOIN FETCH r.items
        WHERE r.id = :id
    """)
    fun findByIdWithItems(@Param("id") id: Long): Optional<LearningRoadmap>
    
    fun findByProfileIdAndTargetPosition(
        profileId: Long,
        targetPosition: String
    ): List<LearningRoadmap>
    
    fun countByProfileId(profileId: Long): Long
}