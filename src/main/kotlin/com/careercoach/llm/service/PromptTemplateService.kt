package com.careercoach.llm.service

import com.careercoach.llm.dto.PromptTemplate
import mu.KotlinLogging
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

/**
 * 프롬프트 템플릿 관리 서비스
 */
@Service
class PromptTemplateService {
    
    private val logger = KotlinLogging.logger {}
    private val templates = mutableMapOf<String, PromptTemplate>()
    
    @PostConstruct
    fun init() {
        logger.info { "Initializing prompt templates" }
        loadDefaultTemplates()
    }
    
    /**
     * 기본 프롬프트 템플릿 로드
     */
    private fun loadDefaultTemplates() {
        // 면접 질문 생성 템플릿
        registerTemplate(
            PromptTemplate(
                name = "interview_question_generation",
                template = """
                    당신은 경력 {years_of_experience}년차 {position} 포지션의 기술 면접관입니다.
                    
                    다음 프로필을 분석하여 맞춤형 면접 질문을 생성해주세요:
                    
                    ## 지원자 프로필
                    이름: {name}
                    경력: {experiences}
                    기술 스택: {skills}
                    
                    ## 요구사항
                    1. 난이도: {difficulty}
                    2. 질문 개수: {question_count}개
                    3. 질문 유형: {question_type}
                    
                    ## 출력 형식
                    각 질문에 대해 다음 정보를 JSON 형식으로 제공해주세요:
                    {
                        "questions": [
                            {
                                "id": "순번",
                                "category": "질문 카테고리",
                                "question": "질문 내용",
                                "difficulty": "난이도 (EASY/MEDIUM/HARD)",
                                "expectedAnswer": "예상 답변의 핵심 포인트",
                                "evaluationCriteria": ["평가 기준 1", "평가 기준 2"],
                                "followUpQuestions": ["후속 질문 1", "후속 질문 2"]
                            }
                        ]
                    }
                """.trimIndent(),
                variables = listOf(
                    "years_of_experience", "position", "name", 
                    "experiences", "skills", "difficulty", 
                    "question_count", "question_type"
                ),
                description = "기술 면접 질문 생성을 위한 템플릿"
            )
        )
        
        // 학습 경로 생성 템플릿
        registerTemplate(
            PromptTemplate(
                name = "learning_path_generation",
                template = """
                    당신은 IT 분야의 커리어 코치입니다.
                    
                    다음 정보를 바탕으로 맞춤형 학습 경로를 생성해주세요:
                    
                    ## 현재 상태
                    현재 포지션: {current_position}
                    경력: {years_of_experience}년
                    보유 기술: {current_skills}
                    
                    ## 목표 상태
                    목표 포지션: {target_position}
                    목표 기업: {target_company}
                    필요 기술: {required_skills}
                    
                    ## 제약 사항
                    학습 가능 시간: 일 {daily_hours}시간
                    목표 달성 기한: {deadline}
                    예산: {budget}
                    
                    ## 출력 형식
                    다음 형식의 JSON으로 응답해주세요:
                    {
                        "roadmap": {
                            "totalDuration": "전체 예상 기간",
                            "phases": [
                                {
                                    "phase": "단계 번호",
                                    "title": "단계 제목",
                                    "duration": "예상 기간",
                                    "skills": ["학습할 기술"],
                                    "resources": [
                                        {
                                            "type": "리소스 타입 (강의/책/프로젝트)",
                                            "title": "리소스 제목",
                                            "url": "리소스 URL",
                                            "estimatedTime": "예상 소요 시간",
                                            "cost": "비용"
                                        }
                                    ],
                                    "milestones": ["달성 지표"],
                                    "projects": ["실습 프로젝트 제안"]
                                }
                            ],
                            "weeklySchedule": {
                                "monday": ["학습 내용"],
                                "tuesday": ["학습 내용"]
                            }
                        }
                    }
                """.trimIndent(),
                variables = listOf(
                    "current_position", "years_of_experience", "current_skills",
                    "target_position", "target_company", "required_skills",
                    "daily_hours", "deadline", "budget"
                ),
                description = "개인 맞춤형 학습 경로 생성 템플릿"
            )
        )
        
        // 이력서 분석 템플릿
        registerTemplate(
            PromptTemplate(
                name = "resume_analysis",
                template = """
                    당신은 채용 전문가입니다. 다음 이력서를 분석해주세요.
                    
                    ## 이력서 내용
                    {resume_content}
                    
                    ## 목표 포지션
                    {target_position}
                    
                    ## 분석 요청 사항
                    1. 강점 분석
                    2. 개선점 제안
                    3. 키워드 최적화
                    4. ATS 점수 예측
                    
                    ## 출력 형식
                    {
                        "score": "전체 점수 (100점 만점)",
                        "strengths": ["강점 1", "강점 2"],
                        "improvements": [
                            {
                                "area": "개선 영역",
                                "current": "현재 상태",
                                "suggestion": "개선 제안",
                                "priority": "우선순위 (HIGH/MEDIUM/LOW)"
                            }
                        ],
                        "keywords": {
                            "missing": ["누락된 키워드"],
                            "recommended": ["추천 키워드"]
                        },
                        "atsScore": {
                            "score": "ATS 점수",
                            "issues": ["ATS 관련 이슈"]
                        }
                    }
                """.trimIndent(),
                variables = listOf("resume_content", "target_position"),
                description = "이력서 분석 및 개선 제안 템플릿"
            )
        )
        
        logger.info { "Loaded ${templates.size} prompt templates" }
    }
    
    /**
     * 템플릿 등록
     */
    fun registerTemplate(template: PromptTemplate) {
        templates[template.name] = template
        logger.debug { "Registered template: ${template.name}" }
    }
    
    /**
     * 템플릿 조회
     */
    fun getTemplate(name: String): PromptTemplate? {
        return templates[name]
    }
    
    /**
     * 템플릿으로 프롬프트 생성
     */
    fun generatePrompt(templateName: String, params: Map<String, Any>): String {
        val template = getTemplate(templateName)
            ?: throw IllegalArgumentException("Template not found: $templateName")
        
        return template.format(params)
    }
    
    /**
     * 모든 템플릿 목록 조회
     */
    fun getAllTemplates(): List<PromptTemplate> {
        return templates.values.toList()
    }
    
    /**
     * 템플릿 삭제
     */
    fun deleteTemplate(name: String) {
        templates.remove(name)
        logger.debug { "Deleted template: $name" }
    }
}