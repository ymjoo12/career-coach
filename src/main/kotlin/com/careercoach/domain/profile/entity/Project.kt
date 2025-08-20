package com.careercoach.domain.profile.entity

import com.careercoach.common.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "projects",
    indexes = [
        Index(name = "idx_projects_experience_id", columnList = "experience_id")
    ]
)
class Project(
    @Column(nullable = false, length = 200)
    var name: String,
    
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    
    @Column(name = "tech_stack", columnDefinition = "TEXT")
    var techStack: String? = null,
    
    @Column(length = 200)
    var role: String? = null,
    
    @Column(name = "start_date")
    var startDate: LocalDate? = null,
    
    @Column(name = "end_date")
    var endDate: LocalDate? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experience_id", nullable = false)
    var experience: Experience? = null
) : BaseEntity() {
    
    fun getTechStackList(): List<String> {
        return techStack?.split(",")?.map { it.trim() } ?: emptyList()
    }
    
    fun setTechStackList(techs: List<String>) {
        techStack = techs.joinToString(", ")
    }
}