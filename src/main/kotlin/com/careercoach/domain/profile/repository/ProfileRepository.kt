package com.careercoach.domain.profile.repository

import com.careercoach.domain.profile.entity.Profile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProfileRepository : JpaRepository<Profile, Long> {
    
    fun findByEmail(email: String): Optional<Profile>
    
    fun existsByEmail(email: String): Boolean
    
    @Query("""
        SELECT DISTINCT p FROM Profile p
        LEFT JOIN FETCH p.experiences e
        LEFT JOIN FETCH e.projects
        WHERE p.id = :id
    """)
    fun findByIdWithExperiences(@Param("id") id: Long): Optional<Profile>
    
    @Query("""
        SELECT DISTINCT p FROM Profile p
        LEFT JOIN FETCH p.technicalSkills
        WHERE p.id = :id
    """)
    fun findByIdWithSkills(@Param("id") id: Long): Optional<Profile>
    
    @Query("""
        SELECT DISTINCT p FROM Profile p
        LEFT JOIN FETCH p.experiences e
        LEFT JOIN FETCH e.projects
        LEFT JOIN FETCH p.technicalSkills
        WHERE p.id = :id
    """)
    fun findByIdWithFullDetails(@Param("id") id: Long): Optional<Profile>
}