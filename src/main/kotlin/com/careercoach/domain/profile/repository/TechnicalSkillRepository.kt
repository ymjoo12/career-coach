package com.careercoach.domain.profile.repository

import com.careercoach.domain.profile.entity.TechnicalSkill
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TechnicalSkillRepository : JpaRepository<TechnicalSkill, Long> {
    
    fun findByProfileId(profileId: Long): List<TechnicalSkill>
    
    fun findByProfileIdAndCategory(profileId: Long, category: String): List<TechnicalSkill>
    
    @Query("""
        SELECT DISTINCT s.category FROM TechnicalSkill s
        WHERE s.profile.id = :profileId
    """)
    fun findCategoriesByProfileId(@Param("profileId") profileId: Long): List<String>
    
    fun deleteByProfileId(profileId: Long)
}