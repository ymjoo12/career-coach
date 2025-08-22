package com.careercoach.interview.service

import com.careercoach.common.exception.ResourceNotFoundException
import com.careercoach.domain.profile.repository.ProfileRepository
import com.careercoach.domain.profile.repository.TechnicalSkillRepository
import com.careercoach.interview.dto.*
import com.careercoach.llm.dto.ChatMessage
import com.careercoach.llm.dto.MessageRole
import com.careercoach.llm.service.LlmService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.careercoach.cache.service.SimilarityBasedCacheService

@Service
@Transactional(readOnly = true)
class InterviewQuestionService(
    private val profileRepository: ProfileRepository,
    private val technicalSkillRepository: TechnicalSkillRepository,
    private val llmService: LlmService,
    private val objectMapper: ObjectMapper,
    private val cacheService: SimilarityBasedCacheService
) {
    
    private val logger = KotlinLogging.logger {}
    
    /**
     * 2단계 면접 질문 생성
     * Stage 1: 프로필 기반 기본 질문 생성
     * Stage 2: 포지션 특화 질문 강화
     */
    @Cacheable(
        value = ["interview-questions"],
        key = "#request.profileId + '_' + #request.targetPosition + '_' + #request.questionCount"
    )
    fun generateInterviewQuestions(request: InterviewQuestionRequest): InterviewQuestionResponse {
        logger.info { "Generating interview questions for profile ${request.profileId}" }
        
        // Check similarity-based cache first
        val cachedQuestions = cacheService.findSimilarCachedQuestions(
            request.profileId,
            request.targetPosition,
            request.targetCompany
        )
        
        if (cachedQuestions != null && cachedQuestions.confidence > 0.7) {
            logger.info { "Using cached questions with similarity ${cachedQuestions.similarityScore}" }
            return try {
                objectMapper.readValue(cachedQuestions.questions, InterviewQuestionResponse::class.java)
            } catch (e: Exception) {
                logger.warn { "Failed to parse cached questions, generating new ones" }
                generateNewQuestions(request)
            }
        }
        
        return generateNewQuestions(request)
    }
    
    private fun generateNewQuestions(request: InterviewQuestionRequest): InterviewQuestionResponse {
        // 프로필 조회
        val profile = profileRepository.findByIdWithExperiences(request.profileId)
            .orElseThrow { ResourceNotFoundException("Profile not found with id: ${request.profileId}") }
        
        val skills = technicalSkillRepository.findByProfileId(request.profileId)
        
        // Stage 1: 프로필 기반 질문 생성
        val baseQuestions = generateBaseQuestions(profile, skills, request)
        
        // Stage 2: 포지션 특화 강화
        val enhancedQuestions = if (request.targetPosition.isNotBlank()) {
            enhanceQuestionsForPosition(baseQuestions, request)
        } else {
            baseQuestions
        }
        
        // 응답 구성
        val response = buildQuestionResponse(request, enhancedQuestions)
        
        // Cache the generated questions
        cacheService.cacheQuestions(
            request.profileId,
            request.targetPosition,
            request.targetCompany,
            objectMapper.writeValueAsString(response),
            mapOf("generated_at" to System.currentTimeMillis())
        )
        
        return response
    }
    
    /**
     * Stage 1: 프로필 기반 기본 질문 생성
     */
    private fun generateBaseQuestions(
        profile: com.careercoach.domain.profile.entity.Profile,
        skills: List<com.careercoach.domain.profile.entity.TechnicalSkill>,
        request: InterviewQuestionRequest
    ): List<InterviewQuestion> {
        
        val experiencesSummary = profile.experiences.joinToString("\n") { exp ->
            "- ${exp.company} (${exp.position}): ${exp.startDate}~${exp.endDate ?: "현재"}\n  주요 업무: ${exp.description}"
        }
        
        val skillsSummary = skills.groupBy { it.category }
            .map { (category, skillList) ->
                "$category: ${skillList.joinToString(", ") { "${it.name} (${it.level})" }}"
            }
            .joinToString("\n")
        
        val projectsSummary = profile.projects.joinToString("\n") { project ->
            "- ${project.name}: ${project.description} (기술: ${project.technologies})"
        }
        
        val params = mapOf(
            "years_of_experience" to profile.yearsOfExperience,
            "position" to (request.targetPosition.ifBlank { profile.currentPosition ?: "개발자" }),
            "name" to profile.name,
            "experiences" to experiencesSummary,
            "skills" to skillsSummary,
            "projects" to projectsSummary,
            "difficulty" to request.difficulty.name,
            "question_count" to request.questionCount,
            "question_type" to request.questionTypes.joinToString(", ") { it.name },
            "focus_areas" to request.focusAreas.joinToString(", ")
        )
        
        val response = llmService.generateFromTemplate(
            templateName = "interview_question_generation",
            params = params,
            maxTokens = 4000,
            temperature = 0.8
        )
        
        return try {
            val jsonResponse = extractJsonFromResponse(response.content)
            val questionData = objectMapper.readValue<Map<String, Any>>(jsonResponse)
            parseQuestions(questionData)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse base questions" }
            generateFallbackQuestions(request)
        }
    }
    
    /**
     * Stage 2: 포지션 특화 질문 강화
     */
    private fun enhanceQuestionsForPosition(
        baseQuestions: List<InterviewQuestion>,
        request: InterviewQuestionRequest
    ): List<InterviewQuestion> {
        
        val enhancementPrompt = buildEnhancementPrompt(baseQuestions, request)
        
        val response = llmService.generateText(
            prompt = enhancementPrompt,
            providerName = null,
            maxTokens = 3000,
            temperature = 0.7
        )
        
        return try {
            val jsonResponse = extractJsonFromResponse(response.content)
            val enhancedData = objectMapper.readValue<Map<String, Any>>(jsonResponse)
            mergeEnhancements(baseQuestions, enhancedData)
        } catch (e: Exception) {
            logger.error(e) { "Failed to enhance questions, returning base questions" }
            baseQuestions
        }
    }
    
    /**
     * 후속 질문 생성
     */
    fun generateFollowUpQuestions(request: FollowUpQuestionRequest): FollowUpQuestionResponse {
        logger.info { "Generating follow-up questions for profile ${request.profileId}" }
        
        val messages = listOf(
            ChatMessage(
                role = MessageRole.SYSTEM,
                content = "당신은 경험 많은 기술 면접관입니다. 지원자의 답변을 분석하고 심화 질문을 생성해주세요."
            ),
            ChatMessage(
                role = MessageRole.USER,
                content = """
                    원래 질문: ${request.originalQuestion}
                    
                    지원자 답변: ${request.userAnswer}
                    
                    위 답변을 바탕으로 ${request.questionCount}개의 후속 질문을 생성해주세요.
                    각 질문은 답변의 깊이를 평가하거나 추가적인 이해도를 확인하기 위한 것이어야 합니다.
                    
                    JSON 형식으로 응답해주세요:
                    {
                        "follow_ups": [
                            {
                                "question": "후속 질문",
                                "purpose": "질문의 목적",
                                "expected_depth": "기대하는 답변 깊이"
                            }
                        ]
                    }
                """.trimIndent()
            )
        )
        
        val response = llmService.generateChatResponse(
            messages = messages,
            maxTokens = 1500,
            temperature = 0.8
        )
        
        return try {
            val jsonResponse = extractJsonFromResponse(response.content)
            val data = objectMapper.readValue<Map<String, Any>>(jsonResponse)
            parseFollowUpQuestions(request, data)
        } catch (e: Exception) {
            logger.error(e) { "Failed to generate follow-up questions" }
            generateFallbackFollowUps(request)
        }
    }
    
    /**
     * 포지션별 강화 프롬프트 생성
     */
    private fun buildEnhancementPrompt(
        baseQuestions: List<InterviewQuestion>,
        request: InterviewQuestionRequest
    ): String {
        val questionsJson = objectMapper.writeValueAsString(
            mapOf("questions" to baseQuestions.map { q ->
                mapOf(
                    "id" to q.id,
                    "question" to q.question,
                    "category" to q.category,
                    "type" to q.type.name
                )
            })
        )
        
        return """
            다음 면접 질문들을 '${request.targetPosition}' 포지션에 더 특화되도록 개선해주세요.
            ${request.targetCompany?.let { "목표 기업: $it" } ?: ""}
            
            현재 질문들:
            $questionsJson
            
            각 질문에 대해:
            1. 포지션 특화 컨텍스트 추가
            2. 기업 문화나 기술 스택 고려 (해당되는 경우)
            3. 실무 시나리오 강화
            
            JSON 형식으로 응답해주세요:
            {
                "enhancements": [
                    {
                        "id": 질문번호,
                        "enhanced_question": "개선된 질문",
                        "context": "추가 컨텍스트",
                        "tags": ["관련 태그"]
                    }
                ]
            }
        """.trimIndent()
    }
    
    /**
     * JSON 응답에서 질문 파싱
     */
    private fun parseQuestions(data: Map<String, Any>): List<InterviewQuestion> {
        val questionsList = data["questions"] as? List<Map<String, Any>> ?: emptyList()
        
        return questionsList.mapIndexedNotNull { index, questionData ->
            try {
                InterviewQuestion(
                    id = (questionData["id"] as? Number)?.toInt() ?: (index + 1),
                    category = questionData["category"] as? String ?: "General",
                    type = parseQuestionType(questionData["type"] as? String),
                    difficulty = parseDifficulty(questionData["difficulty"] as? String),
                    question = questionData["question"] as? String ?: "",
                    context = questionData["context"] as? String,
                    expectedAnswer = questionData["expectedAnswer"] as? String 
                        ?: questionData["expected_answer"] as? String ?: "",
                    evaluationCriteria = (questionData["evaluationCriteria"] as? List<String>)
                        ?: (questionData["evaluation_criteria"] as? List<String>) 
                        ?: emptyList(),
                    followUpQuestions = (questionData["followUpQuestions"] as? List<String>)
                        ?: (questionData["follow_up_questions"] as? List<String>) 
                        ?: emptyList(),
                    tags = (questionData["tags"] as? List<String>) ?: emptyList()
                )
            } catch (e: Exception) {
                logger.error(e) { "Failed to parse question at index $index" }
                null
            }
        }
    }
    
    /**
     * 강화된 질문 병합
     */
    private fun mergeEnhancements(
        baseQuestions: List<InterviewQuestion>,
        enhancedData: Map<String, Any>
    ): List<InterviewQuestion> {
        val enhancements = enhancedData["enhancements"] as? List<Map<String, Any>> ?: emptyList()
        val enhancementMap = enhancements.associateBy { it["id"] as? Int ?: 0 }
        
        return baseQuestions.map { question ->
            val enhancement = enhancementMap[question.id]
            if (enhancement != null) {
                question.copy(
                    question = enhancement["enhanced_question"] as? String ?: question.question,
                    context = enhancement["context"] as? String ?: question.context,
                    tags = (enhancement["tags"] as? List<String>) ?: question.tags
                )
            } else {
                question
            }
        }
    }
    
    /**
     * 후속 질문 파싱
     */
    private fun parseFollowUpQuestions(
        request: FollowUpQuestionRequest,
        data: Map<String, Any>
    ): FollowUpQuestionResponse {
        val followUps = data["follow_ups"] as? List<Map<String, Any>> ?: emptyList()
        
        return FollowUpQuestionResponse(
            originalQuestion = request.originalQuestion,
            userAnswer = request.userAnswer,
            followUpQuestions = followUps.map { fu ->
                FollowUpQuestion(
                    question = fu["question"] as? String ?: "",
                    purpose = fu["purpose"] as? String ?: "",
                    expectedDepth = fu["expected_depth"] as? String ?: ""
                )
            }
        )
    }
    
    /**
     * 응답 구성
     */
    private fun buildQuestionResponse(
        request: InterviewQuestionRequest,
        questions: List<InterviewQuestion>
    ): InterviewQuestionResponse {
        val difficultyDist = questions.groupingBy { it.difficulty.name }.eachCount()
        val typeDist = questions.groupingBy { it.type.name }.eachCount()
        
        return InterviewQuestionResponse(
            profileId = request.profileId,
            targetPosition = request.targetPosition,
            targetCompany = request.targetCompany,
            questions = questions,
            difficultyDistribution = difficultyDist,
            typeDistribution = typeDist
        )
    }
    
    /**
     * JSON 추출 헬퍼
     */
    private fun extractJsonFromResponse(content: String): String {
        // JSON 블록 찾기
        val jsonStart = content.indexOf("{")
        val jsonEnd = content.lastIndexOf("}")
        
        return if (jsonStart >= 0 && jsonEnd > jsonStart) {
            content.substring(jsonStart, jsonEnd + 1)
        } else {
            content
        }
    }
    
    private fun parseQuestionType(type: String?): QuestionType {
        return try {
            QuestionType.valueOf(type?.uppercase() ?: "TECHNICAL")
        } catch (e: Exception) {
            QuestionType.TECHNICAL
        }
    }
    
    private fun parseDifficulty(difficulty: String?): QuestionDifficulty {
        return try {
            QuestionDifficulty.valueOf(difficulty?.uppercase() ?: "MEDIUM")
        } catch (e: Exception) {
            QuestionDifficulty.MEDIUM
        }
    }
    
    /**
     * 폴백 질문 생성 (LLM 실패 시)
     */
    private fun generateFallbackQuestions(request: InterviewQuestionRequest): List<InterviewQuestion> {
        logger.warn { "Generating fallback questions" }
        
        return listOf(
            InterviewQuestion(
                id = 1,
                category = "Technical",
                type = QuestionType.TECHNICAL,
                difficulty = QuestionDifficulty.MEDIUM,
                question = "본인이 가장 자신 있는 기술 스택에 대해 설명해주세요.",
                expectedAnswer = "기술에 대한 깊이 있는 이해도와 실무 경험",
                evaluationCriteria = listOf("기술 이해도", "실무 적용 경험", "문제 해결 능력"),
                followUpQuestions = listOf("해당 기술로 해결한 문제 사례는?", "다른 기술과 비교한 장단점은?")
            ),
            InterviewQuestion(
                id = 2,
                category = "Behavioral",
                type = QuestionType.BEHAVIORAL,
                difficulty = QuestionDifficulty.MEDIUM,
                question = "팀 프로젝트에서 갈등을 해결한 경험을 공유해주세요.",
                expectedAnswer = "구체적인 상황, 행동, 결과를 포함한 답변",
                evaluationCriteria = listOf("커뮤니케이션 능력", "문제 해결 접근법", "팀워크"),
                followUpQuestions = listOf("다시 그 상황이 온다면?", "배운 점은 무엇인가요?")
            )
        )
    }
    
    private fun generateFallbackFollowUps(request: FollowUpQuestionRequest): FollowUpQuestionResponse {
        return FollowUpQuestionResponse(
            originalQuestion = request.originalQuestion,
            userAnswer = request.userAnswer,
            followUpQuestions = listOf(
                FollowUpQuestion(
                    question = "조금 더 구체적인 예시를 들어 설명해주실 수 있나요?",
                    purpose = "답변의 구체성 확인",
                    expectedDepth = "실제 경험 기반 구체적 사례"
                ),
                FollowUpQuestion(
                    question = "이와 관련된 다른 접근 방법은 무엇이 있을까요?",
                    purpose = "다양한 관점 이해도 확인",
                    expectedDepth = "대안적 해결책에 대한 이해"
                )
            )
        )
    }
}