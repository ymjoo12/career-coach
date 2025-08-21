package com.careercoach.learning.service

import com.careercoach.common.exception.ResourceNotFoundException
import com.careercoach.domain.profile.repository.ProfileRepository
import com.careercoach.domain.profile.repository.TechnicalSkillRepository
import com.careercoach.learning.dto.*
import com.careercoach.llm.service.LlmService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class LearningPathService(
    private val profileRepository: ProfileRepository,
    private val technicalSkillRepository: TechnicalSkillRepository,
    private val llmService: LlmService,
    private val objectMapper: ObjectMapper
) {
    
    private val logger = KotlinLogging.logger {}
    
    /**
     * 맞춤형 학습 경로 생성
     */
    @Cacheable(
        value = ["learning-paths"],
        key = "#request.profileId + '_' + #request.targetPosition + '_' + #request.dailyStudyHours"
    )
    fun generateLearningPath(request: LearningPathRequest): LearningPathResponse {
        logger.info { "Generating learning path for profile ${request.profileId}" }
        
        // 프로필 조회
        val profile = profileRepository.findByIdWithExperiences(request.profileId)
            .orElseThrow { ResourceNotFoundException("Profile not found with id: ${request.profileId}") }
        
        val skills = technicalSkillRepository.findByProfileId(request.profileId)
        
        // 스킬 갭 분석
        val skillGapAnalysis = analyzeSkillGap(profile, skills, request)
        
        // 학습 로드맵 생성
        val roadmap = generateRoadmap(profile, skillGapAnalysis, request)
        
        // 응답 구성
        return buildLearningPathResponse(request, roadmap, skillGapAnalysis)
    }
    
    /**
     * 스킬 갭 분석
     */
    fun analyzeSkillGap(request: SkillGapAnalysisRequest): SkillGapAnalysis {
        logger.info { "Analyzing skill gap for profile ${request.profileId}" }
        
        val profile = profileRepository.findByIdWithExperiences(request.profileId)
            .orElseThrow { ResourceNotFoundException("Profile not found with id: ${request.profileId}") }
        
        val skills = technicalSkillRepository.findByProfileId(request.profileId)
        
        val learningRequest = LearningPathRequest(
            profileId = request.profileId,
            targetPosition = request.targetPosition,
            targetCompany = request.targetCompany
        )
        
        return analyzeSkillGap(profile, skills, learningRequest)
    }
    
    /**
     * 스킬 갭 분석 (내부)
     */
    private fun analyzeSkillGap(
        profile: com.careercoach.domain.profile.entity.Profile,
        currentSkills: List<com.careercoach.domain.profile.entity.TechnicalSkill>,
        request: LearningPathRequest
    ): SkillGapAnalysis {
        
        val skillsSummary = currentSkills.groupBy { it.category }
            .map { (category, skillList) ->
                "$category: ${skillList.joinToString(", ") { "${it.name} (${it.level})" }}"
            }
            .joinToString("\n")
        
        val params = mapOf(
            "current_position" to (profile.currentPosition ?: "개발자"),
            "years_of_experience" to profile.yearsOfExperience,
            "current_skills" to skillsSummary,
            "target_position" to request.targetPosition,
            "target_company" to (request.targetCompany ?: "일반 기업"),
            "target_level" to request.targetLevel.name
        )
        
        val prompt = """
            현재 프로필과 목표 포지션을 분석하여 스킬 갭을 파악해주세요.
            
            현재 상태:
            - 포지션: ${params["current_position"]}
            - 경력: ${params["years_of_experience"]}년
            - 보유 스킬: ${params["current_skills"]}
            
            목표:
            - 포지션: ${params["target_position"]}
            - 기업: ${params["target_company"]}
            - 레벨: ${params["target_level"]}
            
            다음 JSON 형식으로 응답해주세요:
            {
                "current_skills": [
                    {
                        "skill": "스킬명",
                        "current_level": "BEGINNER/INTERMEDIATE/ADVANCED/EXPERT",
                        "years_of_experience": 숫자
                    }
                ],
                "required_skills": [
                    {
                        "skill": "스킬명",
                        "required_level": "BEGINNER/INTERMEDIATE/ADVANCED/EXPERT",
                        "importance": "CRITICAL/HIGH/MEDIUM/LOW",
                        "market_demand": "VERY_HIGH/HIGH/MEDIUM/LOW"
                    }
                ],
                "gap_skills": [
                    {
                        "skill": "스킬명",
                        "current_level": "현재 레벨 또는 null",
                        "required_level": "필요 레벨",
                        "gap_size": "LARGE/MEDIUM/SMALL",
                        "estimated_time_to_learn": 주 단위 숫자,
                        "priority": "CRITICAL/HIGH/MEDIUM/LOW"
                    }
                ],
                "transferable_skills": ["전이 가능한 스킬"],
                "priority_areas": ["우선 학습 영역"]
            }
        """.trimIndent()
        
        val response = llmService.generateText(
            prompt = prompt,
            maxTokens = 2000,
            temperature = 0.7
        )
        
        return try {
            val jsonResponse = extractJsonFromResponse(response.content)
            parseSkillGapAnalysis(objectMapper.readValue(jsonResponse))
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse skill gap analysis" }
            generateFallbackSkillGap(currentSkills, request)
        }
    }
    
    /**
     * 학습 로드맵 생성
     */
    private fun generateRoadmap(
        profile: com.careercoach.domain.profile.entity.Profile,
        skillGap: SkillGapAnalysis,
        request: LearningPathRequest
    ): LearningRoadmap {
        
        val gapSkillsJson = objectMapper.writeValueAsString(skillGap.gapSkills)
        val budget = request.budgetMonthly?.let { "월 ${it}원" } ?: "제한 없음"
        val deadline = request.deadlineMonths?.let { "${it}개월 이내" } ?: "제한 없음"
        
        val params = mapOf(
            "target_position" to request.targetPosition,
            "target_company" to (request.targetCompany ?: ""),
            "daily_hours" to request.dailyStudyHours,
            "deadline" to deadline,
            "budget" to budget,
            "learning_style" to request.preferredLearningStyle.name,
            "focus_areas" to request.focusAreas.joinToString(", "),
            "gap_skills" to gapSkillsJson
        )
        
        val response = llmService.generateFromTemplate(
            templateName = "learning_path_generation",
            params = params,
            maxTokens = 4000,
            temperature = 0.8
        )
        
        return try {
            val jsonResponse = extractJsonFromResponse(response.content)
            val roadmapData = objectMapper.readValue<Map<String, Any>>(jsonResponse)
            parseRoadmap(roadmapData["roadmap"] as? Map<String, Any> ?: roadmapData)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse learning roadmap" }
            generateFallbackRoadmap(skillGap, request)
        }
    }
    
    /**
     * 학습 경로 응답 구성
     */
    private fun buildLearningPathResponse(
        request: LearningPathRequest,
        roadmap: LearningRoadmap,
        skillGap: SkillGapAnalysis
    ): LearningPathResponse {
        
        val totalCost = roadmap.phases.sumOf { phase ->
            phase.resources.sumOf { it.cost ?: 0 }
        }
        
        val estimatedMonths = roadmap.totalDurationMonths
        
        return LearningPathResponse(
            profileId = request.profileId,
            targetPosition = request.targetPosition,
            targetCompany = request.targetCompany,
            roadmap = roadmap,
            skillGapAnalysis = skillGap,
            estimatedCompletionMonths = estimatedMonths,
            totalEstimatedCost = if (totalCost > 0) totalCost else null
        )
    }
    
    /**
     * 스킬 갭 분석 파싱
     */
    private fun parseSkillGapAnalysis(data: Map<String, Any>): SkillGapAnalysis {
        val currentSkills = (data["current_skills"] as? List<Map<String, Any>>)?.map { skill ->
            SkillAssessment(
                skill = skill["skill"] as? String ?: "",
                currentLevel = skill["current_level"] as? String ?: "BEGINNER",
                yearsOfExperience = (skill["years_of_experience"] as? Number)?.toDouble() ?: 0.0
            )
        } ?: emptyList()
        
        val requiredSkills = (data["required_skills"] as? List<Map<String, Any>>)?.map { skill ->
            SkillRequirement(
                skill = skill["skill"] as? String ?: "",
                requiredLevel = skill["required_level"] as? String ?: "INTERMEDIATE",
                importance = skill["importance"] as? String ?: "MEDIUM",
                marketDemand = skill["market_demand"] as? String ?: "MEDIUM"
            )
        } ?: emptyList()
        
        val gapSkills = (data["gap_skills"] as? List<Map<String, Any>>)?.map { gap ->
            SkillGap(
                skill = gap["skill"] as? String ?: "",
                currentLevel = gap["current_level"] as? String,
                requiredLevel = gap["required_level"] as? String ?: "INTERMEDIATE",
                gapSize = gap["gap_size"] as? String ?: "MEDIUM",
                estimatedTimeToLearn = (gap["estimated_time_to_learn"] as? Number)?.toInt() ?: 4,
                priority = gap["priority"] as? String ?: "MEDIUM"
            )
        } ?: emptyList()
        
        return SkillGapAnalysis(
            currentSkills = currentSkills,
            requiredSkills = requiredSkills,
            gapSkills = gapSkills,
            transferableSkills = (data["transferable_skills"] as? List<String>) ?: emptyList(),
            priorityAreas = (data["priority_areas"] as? List<String>) ?: emptyList()
        )
    }
    
    /**
     * 로드맵 파싱
     */
    private fun parseRoadmap(data: Map<String, Any>): LearningRoadmap {
        val phases = (data["phases"] as? List<Map<String, Any>>)?.mapIndexed { index, phaseData ->
            LearningPhase(
                phaseNumber = (phaseData["phase"] as? Number)?.toInt() ?: (index + 1),
                title = phaseData["title"] as? String ?: "Phase ${index + 1}",
                description = phaseData["description"] as? String ?: "",
                durationWeeks = ((phaseData["duration"] as? String)?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 4),
                skills = (phaseData["skills"] as? List<String>) ?: emptyList(),
                resources = parseResources(phaseData["resources"] as? List<Map<String, Any>>),
                projects = parseProjects(phaseData["projects"] as? List<Any>),
                completionCriteria = (phaseData["milestones"] as? List<String>) 
                    ?: (phaseData["completion_criteria"] as? List<String>) ?: emptyList()
            )
        } ?: emptyList()
        
        val totalDuration = (data["totalDuration"] as? String)
            ?.replace(Regex("[^0-9]"), "")?.toIntOrNull() 
            ?: phases.sumOf { it.durationWeeks / 4 }
        
        val weeklySchedule = parseWeeklySchedule(data["weeklySchedule"] as? Map<String, Any>)
        
        val milestones = phases.mapIndexed { index, phase ->
            Milestone(
                week = phases.take(index + 1).sumOf { it.durationWeeks },
                title = phase.title,
                description = "Complete ${phase.title}",
                deliverables = phase.completionCriteria
            )
        }
        
        return LearningRoadmap(
            totalDurationMonths = totalDuration,
            phases = phases,
            weeklySchedule = weeklySchedule,
            milestones = milestones
        )
    }
    
    /**
     * 리소스 파싱
     */
    private fun parseResources(resourcesData: List<Map<String, Any>>?): List<LearningResource> {
        return resourcesData?.map { resource ->
            val cost = (resource["cost"] as? String)
                ?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
            
            LearningResource(
                type = parseResourceType(resource["type"] as? String),
                title = resource["title"] as? String ?: "",
                description = resource["description"] as? String,
                url = resource["url"] as? String,
                platform = resource["platform"] as? String,
                estimatedHours = (resource["estimatedTime"] as? String)
                    ?.replace(Regex("[^0-9]"), "")?.toIntOrNull() 
                    ?: (resource["estimated_hours"] as? Number)?.toInt() ?: 10,
                cost = if (cost > 0) cost else null,
                difficulty = resource["difficulty"] as? String ?: "INTERMEDIATE",
                isFree = cost == 0,
                language = resource["language"] as? String ?: "ko"
            )
        } ?: emptyList()
    }
    
    /**
     * 프로젝트 파싱
     */
    private fun parseProjects(projectsData: List<Any>?): List<PracticeProject> {
        return projectsData?.mapNotNull { projectItem ->
            when (projectItem) {
                is String -> PracticeProject(
                    title = projectItem,
                    description = projectItem,
                    technologies = emptyList(),
                    estimatedHours = 20,
                    difficulty = "INTERMEDIATE",
                    learningOutcomes = listOf("실습 완료")
                )
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val project = projectItem as Map<String, Any>
                    PracticeProject(
                        title = project["title"] as? String ?: "",
                        description = project["description"] as? String ?: "",
                        technologies = (project["technologies"] as? List<String>) ?: emptyList(),
                        estimatedHours = (project["estimated_hours"] as? Number)?.toInt() ?: 20,
                        difficulty = project["difficulty"] as? String ?: "INTERMEDIATE",
                        learningOutcomes = (project["learning_outcomes"] as? List<String>) 
                            ?: listOf("프로젝트 완료")
                    )
                }
                else -> null
            }
        } ?: emptyList()
    }
    
    /**
     * 주간 스케줄 파싱
     */
    private fun parseWeeklySchedule(scheduleData: Map<String, Any>?): WeeklySchedule? {
        if (scheduleData == null) return null
        
        return WeeklySchedule(
            monday = (scheduleData["monday"] as? List<String>) ?: emptyList(),
            tuesday = (scheduleData["tuesday"] as? List<String>) ?: emptyList(),
            wednesday = (scheduleData["wednesday"] as? List<String>) ?: emptyList(),
            thursday = (scheduleData["thursday"] as? List<String>) ?: emptyList(),
            friday = (scheduleData["friday"] as? List<String>) ?: emptyList(),
            saturday = (scheduleData["saturday"] as? List<String>) ?: emptyList(),
            sunday = (scheduleData["sunday"] as? List<String>) ?: emptyList()
        )
    }
    
    private fun parseResourceType(type: String?): ResourceType {
        return try {
            ResourceType.valueOf(type?.uppercase() ?: "COURSE")
        } catch (e: Exception) {
            ResourceType.COURSE
        }
    }
    
    /**
     * JSON 추출 헬퍼
     */
    private fun extractJsonFromResponse(content: String): String {
        val jsonStart = content.indexOf("{")
        val jsonEnd = content.lastIndexOf("}")
        
        return if (jsonStart >= 0 && jsonEnd > jsonStart) {
            content.substring(jsonStart, jsonEnd + 1)
        } else {
            content
        }
    }
    
    /**
     * 폴백 스킬 갭 분석
     */
    private fun generateFallbackSkillGap(
        currentSkills: List<com.careercoach.domain.profile.entity.TechnicalSkill>,
        request: LearningPathRequest
    ): SkillGapAnalysis {
        logger.warn { "Generating fallback skill gap analysis" }
        
        val currentAssessments = currentSkills.map { skill ->
            SkillAssessment(
                skill = skill.name,
                currentLevel = skill.level?.name ?: "INTERMEDIATE",
                yearsOfExperience = skill.yearsOfExperience.toDouble()
            )
        }
        
        return SkillGapAnalysis(
            currentSkills = currentAssessments,
            requiredSkills = listOf(
                SkillRequirement("Spring Boot", "EXPERT", "CRITICAL", "HIGH"),
                SkillRequirement("Kubernetes", "ADVANCED", "HIGH", "VERY_HIGH"),
                SkillRequirement("System Design", "ADVANCED", "CRITICAL", "HIGH")
            ),
            gapSkills = listOf(
                SkillGap("Kubernetes", null, "ADVANCED", "LARGE", 12, "HIGH"),
                SkillGap("System Design", "BEGINNER", "ADVANCED", "MEDIUM", 8, "CRITICAL")
            ),
            transferableSkills = listOf("Problem Solving", "Team Collaboration"),
            priorityAreas = listOf("Cloud Native", "System Architecture")
        )
    }
    
    /**
     * 폴백 로드맵 생성
     */
    private fun generateFallbackRoadmap(
        skillGap: SkillGapAnalysis,
        request: LearningPathRequest
    ): LearningRoadmap {
        logger.warn { "Generating fallback learning roadmap" }
        
        return LearningRoadmap(
            totalDurationMonths = 6,
            phases = listOf(
                LearningPhase(
                    phaseNumber = 1,
                    title = "Foundation Building",
                    description = "기초 역량 강화",
                    durationWeeks = 8,
                    skills = listOf("Spring Boot Advanced", "JPA Optimization"),
                    resources = listOf(
                        LearningResource(
                            type = ResourceType.COURSE,
                            title = "Spring Boot 마스터 클래스",
                            description = "Spring Boot 심화 과정",
                            url = "https://example.com",
                            platform = "Udemy",
                            estimatedHours = 40,
                            cost = 50000,
                            difficulty = "ADVANCED",
                            isFree = false
                        )
                    ),
                    projects = listOf(
                        PracticeProject(
                            title = "E-commerce API 구축",
                            description = "실전 프로젝트",
                            technologies = listOf("Spring Boot", "PostgreSQL"),
                            estimatedHours = 60,
                            difficulty = "ADVANCED",
                            learningOutcomes = listOf("REST API 설계", "인증/인가 구현")
                        )
                    ),
                    completionCriteria = listOf("프로젝트 완성", "코드 리뷰 통과")
                ),
                LearningPhase(
                    phaseNumber = 2,
                    title = "Advanced Topics",
                    description = "심화 주제 학습",
                    durationWeeks = 16,
                    skills = skillGap.gapSkills.map { it.skill },
                    resources = emptyList(),
                    projects = emptyList(),
                    completionCriteria = listOf("기술 면접 준비 완료")
                )
            ),
            weeklySchedule = WeeklySchedule(
                monday = listOf("온라인 강의 2시간"),
                tuesday = listOf("실습 프로젝트 2시간"),
                wednesday = listOf("온라인 강의 2시간"),
                thursday = listOf("실습 프로젝트 2시간"),
                friday = listOf("복습 및 정리 2시간"),
                saturday = listOf("프로젝트 집중 작업 4시간"),
                sunday = listOf("휴식")
            ),
            milestones = listOf(
                Milestone(
                    week = 8,
                    title = "Phase 1 Complete",
                    description = "기초 과정 완료",
                    deliverables = listOf("포트폴리오 프로젝트 1개")
                )
            )
        )
    }
}