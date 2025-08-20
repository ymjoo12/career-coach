package com.careercoach.domain.learning.entity

import com.careercoach.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "learning_roadmap_items",
    indexes = [
        Index(name = "idx_roadmap_items_roadmap_id", columnList = "roadmap_id")
    ]
)
class LearningRoadmapItem(
    @Column(nullable = false, length = 200)
    var title: String,
    
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    
    @Column(length = 100)
    var category: String? = null,
    
    @Column(name = "duration_weeks")
    var durationWeeks: Int? = null,
    
    @Column(columnDefinition = "TEXT")
    var resources: String? = null,
    
    @Column(name = "order_index")
    var orderIndex: Int = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roadmap_id", nullable = false)
    var roadmap: LearningRoadmap? = null
) : BaseEntity() {
    
    fun getResourcesList(): List<String> {
        return resources?.split("\n")?.map { it.trim() } ?: emptyList()
    }
    
    fun setResourcesList(resourceList: List<String>) {
        resources = resourceList.joinToString("\n")
    }
    
    fun getDurationInMonths(): Double {
        return (durationWeeks ?: 0) / 4.0
    }
}