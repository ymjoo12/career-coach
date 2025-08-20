package com.careercoach.domain.profile.entity

import com.careercoach.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate
import java.time.Period

@Entity
@Table(
    name = "experiences",
    indexes = [
        Index(name = "idx_experiences_profile_id", columnList = "profile_id")
    ]
)
class Experience(
    @Column(nullable = false, length = 200)
    var company: String,
    
    @Column(nullable = false, length = 200)
    var position: String,
    
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,
    
    @Column(name = "end_date")
    var endDate: LocalDate? = null,
    
    @Column(name = "is_current")
    var isCurrent: Boolean = false,
    
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    var profile: Profile? = null,
    
    @OneToMany(mappedBy = "experience", cascade = [CascadeType.ALL], orphanRemoval = true)
    val projects: MutableList<Project> = mutableListOf()
) : BaseEntity() {
    
    fun addProject(project: Project) {
        projects.add(project)
        project.experience = this
    }
    
    fun removeProject(project: Project) {
        projects.remove(project)
        project.experience = null
    }
    
    fun calculateDuration(): Int {
        val end = if (isCurrent) LocalDate.now() else (endDate ?: startDate)
        val period = Period.between(startDate, end)
        return period.years * 12 + period.months
    }
    
    fun getDurationInYears(): Double {
        return calculateDuration() / 12.0
    }
    
    fun getFormattedPeriod(): String {
        val endStr = if (isCurrent) "현재" else endDate?.toString() ?: ""
        return "$startDate ~ $endStr"
    }
}