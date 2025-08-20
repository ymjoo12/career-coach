package com.careercoach.domain.learning.entity

import com.careercoach.common.entity.BaseEntity
import com.careercoach.domain.profile.entity.Profile
import jakarta.persistence.*

@Entity
@Table(
    name = "learning_roadmaps",
    indexes = [
        Index(name = "idx_roadmaps_profile_id", columnList = "profile_id")
    ]
)
class LearningRoadmap(
    @Column(name = "target_position", length = 200)
    var targetPosition: String? = null,
    
    @Column(name = "total_duration_weeks")
    var totalDurationWeeks: Int? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    var profile: Profile? = null,
    
    @OneToMany(mappedBy = "roadmap", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<LearningRoadmapItem> = mutableListOf()
) : BaseEntity() {
    
    fun addItem(item: LearningRoadmapItem) {
        items.add(item)
        item.roadmap = this
    }
    
    fun removeItem(item: LearningRoadmapItem) {
        items.remove(item)
        item.roadmap = null
    }
    
    fun calculateTotalDuration(): Int {
        return items.sumOf { it.durationWeeks ?: 0 }
    }
    
    fun getItemsByCategory(category: String): List<LearningRoadmapItem> {
        return items.filter { it.category == category }
    }
    
    fun getSortedItems(): List<LearningRoadmapItem> {
        return items.sortedBy { it.orderIndex }
    }
}