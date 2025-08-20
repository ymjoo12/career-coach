package com.careercoach.domain.profile.service

import com.careercoach.common.exception.DuplicateResourceException
import com.careercoach.common.exception.ResourceNotFoundException
import com.careercoach.domain.profile.dto.*
import com.careercoach.domain.profile.entity.Experience
import com.careercoach.domain.profile.entity.Profile
import com.careercoach.domain.profile.entity.Project
import com.careercoach.domain.profile.entity.TechnicalSkill
import com.careercoach.domain.profile.repository.ProfileRepository
import com.careercoach.domain.profile.repository.ExperienceRepository
import com.careercoach.domain.profile.repository.TechnicalSkillRepository
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProfileService(
    private val profileRepository: ProfileRepository,
    private val experienceRepository: ExperienceRepository,
    private val technicalSkillRepository: TechnicalSkillRepository
) {
    private val logger = KotlinLogging.logger {}
    
    @Transactional
    fun createProfile(request: CreateProfileRequest): ProfileResponse {
        logger.debug { "Creating profile for email: ${request.email}" }
        
        // 이메일 중복 체크
        if (profileRepository.existsByEmail(request.email)) {
            throw DuplicateResourceException(
                "이미 존재하는 이메일입니다: ${request.email}",
                mapOf("email" to request.email)
            )
        }
        
        val profile = Profile(
            name = request.name,
            email = request.email,
            phone = request.phone,
            summary = request.summary,
            yearsOfExperience = request.yearsOfExperience
        )
        
        val savedProfile = profileRepository.save(profile)
        logger.info { "Profile created with ID: ${savedProfile.id}" }
        
        return ProfileResponse.from(savedProfile)
    }
    
    @Transactional
    fun updateProfile(id: Long, request: UpdateProfileRequest): ProfileResponse {
        logger.debug { "Updating profile with ID: $id" }
        
        val profile = profileRepository.findById(id)
            .orElseThrow { 
                ResourceNotFoundException(
                    "프로필을 찾을 수 없습니다: $id",
                    mapOf("profileId" to id)
                )
            }
        
        request.name?.let { profile.name = it }
        request.phone?.let { profile.phone = it }
        request.summary?.let { profile.summary = it }
        request.yearsOfExperience?.let { profile.yearsOfExperience = it }
        
        val updatedProfile = profileRepository.save(profile)
        logger.info { "Profile updated with ID: ${updatedProfile.id}" }
        
        return ProfileResponse.from(updatedProfile)
    }
    
    fun getProfile(id: Long): ProfileResponse {
        logger.debug { "Getting profile with ID: $id" }
        
        // First fetch profile with experiences
        val profile = profileRepository.findByIdWithExperiences(id)
            .orElseThrow { 
                ResourceNotFoundException(
                    "프로필을 찾을 수 없습니다: $id",
                    mapOf("profileId" to id)
                )
            }
        
        // Then fetch technical skills separately and get the skills list
        val skills = technicalSkillRepository.findByProfileId(id)
        
        // Create ProfileResponse with both experiences and skills
        return ProfileResponse.from(profile, skills)
    }
    
    fun getProfileByEmail(email: String): ProfileResponse {
        logger.debug { "Getting profile by email: $email" }
        
        val profile = profileRepository.findByEmail(email)
            .orElseThrow { 
                ResourceNotFoundException(
                    "프로필을 찾을 수 없습니다: $email",
                    mapOf("email" to email)
                )
            }
        
        return ProfileResponse.from(profile)
    }
    
    fun getProfiles(pageable: Pageable): Page<ProfileSummaryResponse> {
        logger.debug { "Getting profiles page: ${pageable.pageNumber}" }
        
        return profileRepository.findAll(pageable)
            .map { ProfileSummaryResponse.from(it) }
    }
    
    @Transactional
    fun deleteProfile(id: Long) {
        logger.debug { "Deleting profile with ID: $id" }
        
        if (!profileRepository.existsById(id)) {
            throw ResourceNotFoundException(
                "프로필을 찾을 수 없습니다: $id",
                mapOf("profileId" to id)
            )
        }
        
        profileRepository.deleteById(id)
        logger.info { "Profile deleted with ID: $id" }
    }
    
    // Experience 관련 메서드들
    @Transactional
    fun addExperience(profileId: Long, request: ExperienceRequest): ExperienceResponse {
        logger.debug { "Adding experience to profile: $profileId" }
        
        val profile = profileRepository.findById(profileId)
            .orElseThrow { 
                ResourceNotFoundException(
                    "프로필을 찾을 수 없습니다: $profileId",
                    mapOf("profileId" to profileId)
                )
            }
        
        val experience = Experience(
            company = request.company,
            position = request.position,
            startDate = request.startDate,
            endDate = request.endDate,
            isCurrent = request.isCurrent,
            description = request.description
        )
        
        profile.addExperience(experience)
        val savedProfile = profileRepository.save(profile)
        
        val savedExperience = savedProfile.experiences.last()
        logger.info { "Experience added with ID: ${savedExperience.id}" }
        
        return ExperienceResponse.from(savedExperience)
    }
    
    @Transactional
    fun updateExperience(profileId: Long, experienceId: Long, request: ExperienceRequest): ExperienceResponse {
        logger.debug { "Updating experience $experienceId for profile: $profileId" }
        
        val experience = experienceRepository.findById(experienceId)
            .orElseThrow { 
                ResourceNotFoundException(
                    "경력을 찾을 수 없습니다: $experienceId",
                    mapOf("experienceId" to experienceId)
                )
            }
        
        if (experience.profile?.id != profileId) {
            throw ResourceNotFoundException(
                "해당 프로필의 경력이 아닙니다",
                mapOf("profileId" to profileId, "experienceId" to experienceId)
            )
        }
        
        experience.apply {
            company = request.company
            position = request.position
            startDate = request.startDate
            endDate = request.endDate
            isCurrent = request.isCurrent
            description = request.description
        }
        
        val savedExperience = experienceRepository.save(experience)
        logger.info { "Experience updated with ID: ${savedExperience.id}" }
        
        return ExperienceResponse.from(savedExperience)
    }
    
    @Transactional
    fun deleteExperience(profileId: Long, experienceId: Long) {
        logger.debug { "Deleting experience $experienceId from profile: $profileId" }
        
        val profile = profileRepository.findById(profileId)
            .orElseThrow { 
                ResourceNotFoundException(
                    "프로필을 찾을 수 없습니다: $profileId",
                    mapOf("profileId" to profileId)
                )
            }
        
        val experience = profile.experiences.find { it.id == experienceId }
            ?: throw ResourceNotFoundException(
                "경력을 찾을 수 없습니다: $experienceId",
                mapOf("experienceId" to experienceId)
            )
        
        profile.removeExperience(experience)
        profileRepository.save(profile)
        
        logger.info { "Experience deleted with ID: $experienceId" }
    }
    
    // TechnicalSkill 관련 메서드들
    @Transactional
    fun addTechnicalSkill(profileId: Long, request: TechnicalSkillRequest): TechnicalSkillResponse {
        logger.debug { "Adding technical skill to profile: $profileId" }
        
        val profile = profileRepository.findById(profileId)
            .orElseThrow { 
                ResourceNotFoundException(
                    "프로필을 찾을 수 없습니다: $profileId",
                    mapOf("profileId" to profileId)
                )
            }
        
        val skill = TechnicalSkill(
            category = request.category,
            name = request.name,
            level = request.level,
            yearsOfExperience = request.yearsOfExperience
        )
        
        profile.addTechnicalSkill(skill)
        val savedProfile = profileRepository.save(profile)
        
        val savedSkill = savedProfile.technicalSkills.last()
        logger.info { "Technical skill added with ID: ${savedSkill.id}" }
        
        return TechnicalSkillResponse.from(savedSkill)
    }
    
    @Transactional
    fun updateTechnicalSkills(profileId: Long, request: BulkTechnicalSkillsRequest): List<TechnicalSkillResponse> {
        logger.debug { "Bulk updating technical skills for profile: $profileId" }
        
        val profile = profileRepository.findByIdWithSkills(profileId)
            .orElseThrow { 
                ResourceNotFoundException(
                    "프로필을 찾을 수 없습니다: $profileId",
                    mapOf("profileId" to profileId)
                )
            }
        
        // 삭제
        request.delete.forEach { skillId ->
            profile.technicalSkills.removeIf { it.id == skillId }
        }
        
        // 수정
        request.update.forEach { updateRequest ->
            profile.technicalSkills.find { it.id == updateRequest.id }?.apply {
                updateRequest.category?.let { category = it }
                updateRequest.name?.let { name = it }
                updateRequest.level?.let { level = it }
                updateRequest.yearsOfExperience?.let { yearsOfExperience = it }
            }
        }
        
        // 추가
        request.add.forEach { addRequest ->
            val skill = TechnicalSkill(
                category = addRequest.category,
                name = addRequest.name,
                level = addRequest.level,
                yearsOfExperience = addRequest.yearsOfExperience
            )
            profile.addTechnicalSkill(skill)
        }
        
        val savedProfile = profileRepository.save(profile)
        logger.info { "Technical skills updated for profile: $profileId" }
        
        return savedProfile.technicalSkills.map { TechnicalSkillResponse.from(it) }
    }
    
    fun getTechnicalSkills(profileId: Long): List<TechnicalSkillResponse> {
        logger.debug { "Getting technical skills for profile: $profileId" }
        
        return technicalSkillRepository.findByProfileId(profileId)
            .map { TechnicalSkillResponse.from(it) }
    }
}