package com.careercoach.domain.profile.controller

import com.careercoach.common.dto.ApiResponse
import com.careercoach.common.util.PageUtils
import com.careercoach.domain.profile.dto.*
import com.careercoach.domain.profile.service.ProfileService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Profile", description = "프로필 관리 API")
@RestController
@RequestMapping("/api/v1/profiles")
class ProfileController(
    private val profileService: ProfileService
) {
    
    @Operation(summary = "프로필 생성", description = "새로운 프로필을 생성합니다")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProfile(
        @Valid @RequestBody request: CreateProfileRequest
    ): ApiResponse<ProfileResponse> {
        val profile = profileService.createProfile(request)
        return ApiResponse.success(profile)
    }
    
    @Operation(summary = "프로필 수정", description = "프로필 정보를 수정합니다")
    @PutMapping("/{id}")
    fun updateProfile(
        @Parameter(description = "프로필 ID") @PathVariable id: Long,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ApiResponse<ProfileResponse> {
        val profile = profileService.updateProfile(id, request)
        return ApiResponse.success(profile)
    }
    
    @Operation(summary = "프로필 조회", description = "프로필 상세 정보를 조회합니다")
    @GetMapping("/{id}")
    fun getProfile(
        @Parameter(description = "프로필 ID") @PathVariable id: Long
    ): ApiResponse<ProfileResponse> {
        val profile = profileService.getProfile(id)
        return ApiResponse.success(profile)
    }
    
    @Operation(summary = "이메일로 프로필 조회", description = "이메일로 프로필을 조회합니다")
    @GetMapping("/email/{email}")
    fun getProfileByEmail(
        @Parameter(description = "이메일") @PathVariable email: String
    ): ApiResponse<ProfileResponse> {
        val profile = profileService.getProfileByEmail(email)
        return ApiResponse.success(profile)
    }
    
    @Operation(summary = "프로필 목록 조회", description = "프로필 목록을 페이징하여 조회합니다")
    @GetMapping
    fun getProfiles(
        @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "createdAt") sortBy: String,
        @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "DESC") sortDirection: Sort.Direction
    ): ApiResponse<Page<ProfileSummaryResponse>> {
        val pageable = PageUtils.createPageable(page, size, sortBy, sortDirection)
        val profiles = profileService.getProfiles(pageable)
        return ApiResponse.success(profiles)
    }
    
    @Operation(summary = "프로필 삭제", description = "프로필을 삭제합니다")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProfile(
        @Parameter(description = "프로필 ID") @PathVariable id: Long
    ): ApiResponse<Unit> {
        profileService.deleteProfile(id)
        return ApiResponse.success()
    }
    
    // Experience 관련 엔드포인트
    @Operation(summary = "경력 추가", description = "프로필에 경력을 추가합니다")
    @PostMapping("/{profileId}/experiences")
    @ResponseStatus(HttpStatus.CREATED)
    fun addExperience(
        @Parameter(description = "프로필 ID") @PathVariable profileId: Long,
        @Valid @RequestBody request: ExperienceRequest
    ): ApiResponse<ExperienceResponse> {
        val experience = profileService.addExperience(profileId, request)
        return ApiResponse.success(experience)
    }
    
    @Operation(summary = "경력 수정", description = "경력 정보를 수정합니다")
    @PutMapping("/{profileId}/experiences/{experienceId}")
    fun updateExperience(
        @Parameter(description = "프로필 ID") @PathVariable profileId: Long,
        @Parameter(description = "경력 ID") @PathVariable experienceId: Long,
        @Valid @RequestBody request: ExperienceRequest
    ): ApiResponse<ExperienceResponse> {
        val experience = profileService.updateExperience(profileId, experienceId, request)
        return ApiResponse.success(experience)
    }
    
    @Operation(summary = "경력 삭제", description = "경력을 삭제합니다")
    @DeleteMapping("/{profileId}/experiences/{experienceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteExperience(
        @Parameter(description = "프로필 ID") @PathVariable profileId: Long,
        @Parameter(description = "경력 ID") @PathVariable experienceId: Long
    ): ApiResponse<Unit> {
        profileService.deleteExperience(profileId, experienceId)
        return ApiResponse.success()
    }
    
    // TechnicalSkill 관련 엔드포인트
    @Operation(summary = "기술 스킬 추가", description = "프로필에 기술 스킬을 추가합니다")
    @PostMapping("/{profileId}/skills")
    @ResponseStatus(HttpStatus.CREATED)
    fun addTechnicalSkill(
        @Parameter(description = "프로필 ID") @PathVariable profileId: Long,
        @Valid @RequestBody request: TechnicalSkillRequest
    ): ApiResponse<TechnicalSkillResponse> {
        val skill = profileService.addTechnicalSkill(profileId, request)
        return ApiResponse.success(skill)
    }
    
    @Operation(summary = "기술 스킬 일괄 수정", description = "기술 스킬을 일괄로 추가/수정/삭제합니다")
    @PutMapping("/{profileId}/skills/bulk")
    fun updateTechnicalSkills(
        @Parameter(description = "프로필 ID") @PathVariable profileId: Long,
        @Valid @RequestBody request: BulkTechnicalSkillsRequest
    ): ApiResponse<List<TechnicalSkillResponse>> {
        val skills = profileService.updateTechnicalSkills(profileId, request)
        return ApiResponse.success(skills)
    }
    
    @Operation(summary = "기술 스킬 목록 조회", description = "프로필의 기술 스킬 목록을 조회합니다")
    @GetMapping("/{profileId}/skills")
    fun getTechnicalSkills(
        @Parameter(description = "프로필 ID") @PathVariable profileId: Long
    ): ApiResponse<List<TechnicalSkillResponse>> {
        val skills = profileService.getTechnicalSkills(profileId)
        return ApiResponse.success(skills)
    }
}