package com.careercoach.domain.profile.entity

import com.careercoach.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "projects")
class Project(
    @Column(nullable = false, length = 200)
    var name: String,
    
    @Column(columnDefinition = "TEXT")
    var description: String,
    
    @Column(name = "start_date")
    var startDate: LocalDate,
    
    @Column(name = "end_date")
    var endDate: LocalDate? = null,
    
    @Column(columnDefinition = "TEXT")
    var technologies: String? = null,
    
    @Column(columnDefinition = "TEXT")
    var role: String? = null,
    
    @Column(columnDefinition = "TEXT")
    var achievements: String? = null,
    
    @Column(length = 500)
    var url: String? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    var profile: Profile? = null
) : BaseEntity() {
    
    fun isOngoing(): Boolean {
        return endDate == null
    }
    
    fun calculateDuration(): Int {
        val end = endDate ?: LocalDate.now()
        return ((end.toEpochDay() - startDate.toEpochDay()) / 30).toInt()
    }
}