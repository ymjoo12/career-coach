package com.careercoach.domain.profile.dto

import com.careercoach.domain.profile.entity.Profile
import com.careercoach.domain.profile.entity.TechnicalSkill
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "프로필 생성 요청")
data class CreateProfileRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(max = 100, message = "이름은 100자 이내여야 합니다")
    @Schema(description = "이름", example = "홍길동")
    val name: String,
    
    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @Schema(description = "이메일", example = "hong@example.com")
    val email: String,
    
    @field:Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다")
    @Schema(description = "전화번호", example = "010-1234-5678")
    val phone: String? = null,
    
    @Schema(description = "자기소개", example = "10년차 백엔드 개발자입니다")
    val summary: String? = null,
    
    @field:Min(0, message = "경력은 0년 이상이어야 합니다")
    @field:Max(50, message = "경력은 50년 이하여야 합니다")
    @Schema(description = "총 경력(년)", example = "10")
    val yearsOfExperience: Int = 0
)

@Schema(description = "프로필 수정 요청")
data class UpdateProfileRequest(
    @field:Size(max = 100, message = "이름은 100자 이내여야 합니다")
    @Schema(description = "이름", example = "홍길동")
    val name: String? = null,
    
    @field:Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다")
    @Schema(description = "전화번호", example = "010-1234-5678")
    val phone: String? = null,
    
    @Schema(description = "자기소개")
    val summary: String? = null,
    
    @field:Min(0, message = "경력은 0년 이상이어야 합니다")
    @field:Max(50, message = "경력은 50년 이하여야 합니다")
    @Schema(description = "총 경력(년)")
    val yearsOfExperience: Int? = null
)

@Schema(description = "프로필 응답")
data class ProfileResponse(
    @Schema(description = "프로필 ID")
    val id: Long,
    
    @Schema(description = "이름")
    val name: String,
    
    @Schema(description = "이메일")
    val email: String,
    
    @Schema(description = "전화번호")
    val phone: String?,
    
    @Schema(description = "자기소개")
    val summary: String?,
    
    @Schema(description = "총 경력(년)")
    val yearsOfExperience: Int,
    
    @Schema(description = "경력 정보 목록")
    val experiences: List<ExperienceResponse> = emptyList(),
    
    @Schema(description = "기술 스킬 목록")
    val technicalSkills: List<TechnicalSkillResponse> = emptyList(),
    
    @Schema(description = "생성일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime,
    
    @Schema(description = "수정일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(profile: Profile): ProfileResponse {
            return ProfileResponse(
                id = profile.id!!,
                name = profile.name,
                email = profile.email,
                phone = profile.phone,
                summary = profile.summary,
                yearsOfExperience = profile.yearsOfExperience,
                experiences = profile.experiences.map { ExperienceResponse.from(it) },
                technicalSkills = profile.technicalSkills.map { TechnicalSkillResponse.from(it) },
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt
            )
        }
        
        fun from(profile: Profile, technicalSkills: List<TechnicalSkill>): ProfileResponse {
            return ProfileResponse(
                id = profile.id!!,
                name = profile.name,
                email = profile.email,
                phone = profile.phone,
                summary = profile.summary,
                yearsOfExperience = profile.yearsOfExperience,
                experiences = profile.experiences.map { ExperienceResponse.from(it) },
                technicalSkills = technicalSkills.map { TechnicalSkillResponse.from(it) },
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt
            )
        }
    }
}

@Schema(description = "프로필 목록 응답")
data class ProfileSummaryResponse(
    @Schema(description = "프로필 ID")
    val id: Long,
    
    @Schema(description = "이름")
    val name: String,
    
    @Schema(description = "이메일")
    val email: String,
    
    @Schema(description = "총 경력(년)")
    val yearsOfExperience: Int,
    
    @Schema(description = "경력 수")
    val experienceCount: Int,
    
    @Schema(description = "기술 스킬 수")
    val skillCount: Int,
    
    @Schema(description = "생성일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(profile: Profile): ProfileSummaryResponse {
            return ProfileSummaryResponse(
                id = profile.id!!,
                name = profile.name,
                email = profile.email,
                yearsOfExperience = profile.yearsOfExperience,
                experienceCount = profile.experiences.size,
                skillCount = profile.technicalSkills.size,
                createdAt = profile.createdAt
            )
        }
    }
}