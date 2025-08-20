package com.careercoach.domain.profile.dto

import com.careercoach.domain.profile.entity.TechnicalSkill
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*

@Schema(description = "기술 스킬 생성/수정 요청")
data class TechnicalSkillRequest(
    @field:NotBlank(message = "카테고리는 필수입니다")
    @field:Size(max = 100, message = "카테고리는 100자 이내여야 합니다")
    @Schema(description = "카테고리", example = "Backend", allowableValues = ["Backend", "Frontend", "Database", "DevOps", "Mobile", "AI/ML", "Other"])
    val category: String,
    
    @field:NotBlank(message = "스킬명은 필수입니다")
    @field:Size(max = 100, message = "스킬명은 100자 이내여야 합니다")
    @Schema(description = "스킬명", example = "Spring Boot")
    val name: String,
    
    @Schema(description = "숙련도", example = "ADVANCED")
    val level: TechnicalSkill.SkillLevel? = TechnicalSkill.SkillLevel.INTERMEDIATE,
    
    @field:Min(0, message = "경험 연수는 0년 이상이어야 합니다")
    @field:Max(50, message = "경험 연수는 50년 이하여야 합니다")
    @Schema(description = "경험 연수", example = "5")
    val yearsOfExperience: Int = 0
)

@Schema(description = "기술 스킬 응답")
data class TechnicalSkillResponse(
    @Schema(description = "스킬 ID")
    val id: Long,
    
    @Schema(description = "카테고리")
    val category: String,
    
    @Schema(description = "스킬명")
    val name: String,
    
    @Schema(description = "숙련도")
    val level: TechnicalSkill.SkillLevel?,
    
    @Schema(description = "숙련도 설명")
    val levelDescription: String,
    
    @Schema(description = "경험 연수")
    val yearsOfExperience: Int
) {
    companion object {
        fun from(skill: TechnicalSkill): TechnicalSkillResponse {
            return TechnicalSkillResponse(
                id = skill.id!!,
                category = skill.category,
                name = skill.name,
                level = skill.level,
                levelDescription = skill.getLevelDescription(),
                yearsOfExperience = skill.yearsOfExperience
            )
        }
    }
}

@Schema(description = "기술 스킬 일괄 수정 요청")
data class BulkTechnicalSkillsRequest(
    @Schema(description = "추가할 스킬 목록")
    val add: List<TechnicalSkillRequest> = emptyList(),
    
    @Schema(description = "수정할 스킬 목록 (ID 포함)")
    val update: List<TechnicalSkillUpdateRequest> = emptyList(),
    
    @Schema(description = "삭제할 스킬 ID 목록")
    val delete: List<Long> = emptyList()
)

@Schema(description = "기술 스킬 수정 요청 (ID 포함)")
data class TechnicalSkillUpdateRequest(
    @field:NotNull(message = "스킬 ID는 필수입니다")
    @Schema(description = "스킬 ID")
    val id: Long,
    
    @Schema(description = "카테고리")
    val category: String? = null,
    
    @Schema(description = "스킬명")
    val name: String? = null,
    
    @Schema(description = "숙련도")
    val level: TechnicalSkill.SkillLevel? = null,
    
    @Schema(description = "경험 연수")
    val yearsOfExperience: Int? = null
)