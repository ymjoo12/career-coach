package com.careercoach.domain.profile.dto

import com.careercoach.domain.profile.entity.Experience
import com.careercoach.domain.profile.entity.Project
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.time.LocalDate

@Schema(description = "경력 생성/수정 요청")
data class ExperienceRequest(
    @field:NotBlank(message = "회사명은 필수입니다")
    @field:Size(max = 200, message = "회사명은 200자 이내여야 합니다")
    @Schema(description = "회사명", example = "삼성전자")
    val company: String,
    
    @field:NotBlank(message = "직책은 필수입니다")
    @field:Size(max = 200, message = "직책은 200자 이내여야 합니다")
    @Schema(description = "직책", example = "시니어 백엔드 개발자")
    val position: String,
    
    @field:NotNull(message = "시작일은 필수입니다")
    @Schema(description = "시작일", example = "2020-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate,
    
    @Schema(description = "종료일 (재직중인 경우 null)", example = "2023-12-31")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val endDate: LocalDate? = null,
    
    @Schema(description = "현재 재직 중 여부", example = "false")
    val isCurrent: Boolean = false,
    
    @Schema(description = "업무 설명")
    val description: String? = null,
    
    @Schema(description = "프로젝트 목록")
    val projects: List<ProjectRequest> = emptyList()
)

@Schema(description = "경력 응답")
data class ExperienceResponse(
    @Schema(description = "경력 ID")
    val id: Long,
    
    @Schema(description = "회사명")
    val company: String,
    
    @Schema(description = "직책")
    val position: String,
    
    @Schema(description = "시작일")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate,
    
    @Schema(description = "종료일")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val endDate: LocalDate?,
    
    @Schema(description = "현재 재직 중 여부")
    val isCurrent: Boolean,
    
    @Schema(description = "업무 설명")
    val description: String?,
    
    @Schema(description = "근무 기간 (개월)")
    val durationMonths: Int,
    
    @Schema(description = "프로젝트 목록")
    val projects: List<ProjectResponse> = emptyList()
) {
    companion object {
        fun from(experience: Experience): ExperienceResponse {
            return ExperienceResponse(
                id = experience.id!!,
                company = experience.company,
                position = experience.position,
                startDate = experience.startDate,
                endDate = experience.endDate,
                isCurrent = experience.isCurrent,
                description = experience.description,
                durationMonths = experience.calculateDuration(),
                projects = experience.projects.map { ProjectResponse.from(it) }
            )
        }
    }
}

@Schema(description = "프로젝트 생성/수정 요청")
data class ProjectRequest(
    @field:NotBlank(message = "프로젝트명은 필수입니다")
    @field:Size(max = 200, message = "프로젝트명은 200자 이내여야 합니다")
    @Schema(description = "프로젝트명", example = "이커머스 플랫폼 구축")
    val name: String,
    
    @Schema(description = "프로젝트 설명")
    val description: String? = null,
    
    @Schema(description = "기술 스택", example = "Spring Boot, Kotlin, PostgreSQL, Redis")
    val techStack: String? = null,
    
    @Schema(description = "담당 역할", example = "백엔드 리드 개발자")
    val role: String? = null,
    
    @Schema(description = "시작일")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate? = null,
    
    @Schema(description = "종료일")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val endDate: LocalDate? = null
)

@Schema(description = "프로젝트 응답")
data class ProjectResponse(
    @Schema(description = "프로젝트 ID")
    val id: Long,
    
    @Schema(description = "프로젝트명")
    val name: String,
    
    @Schema(description = "프로젝트 설명")
    val description: String?,
    
    @Schema(description = "기술 스택")
    val techStack: String?,
    
    @Schema(description = "담당 역할")
    val role: String?,
    
    @Schema(description = "시작일")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val startDate: LocalDate?,
    
    @Schema(description = "종료일")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val endDate: LocalDate?
) {
    companion object {
        fun from(project: Project): ProjectResponse {
            return ProjectResponse(
                id = project.id!!,
                name = project.name,
                description = project.description,
                techStack = project.techStack,
                role = project.role,
                startDate = project.startDate,
                endDate = project.endDate
            )
        }
    }
}