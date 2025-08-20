package com.careercoach.domain.profile.repository

import com.careercoach.domain.profile.entity.Experience
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ExperienceRepository : JpaRepository<Experience, Long> {
    
    fun findByProfileId(profileId: Long): List<Experience>
    
    @Query("""
        SELECT e FROM Experience e
        WHERE e.profile.id = :profileId
        ORDER BY e.startDate DESC
    """)
    fun findByProfileIdOrderByStartDateDesc(@Param("profileId") profileId: Long): List<Experience>
    
    fun countByProfileId(profileId: Long): Long
}