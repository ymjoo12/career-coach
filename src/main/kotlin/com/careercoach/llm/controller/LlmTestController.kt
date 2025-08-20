package com.careercoach.llm.controller

import com.careercoach.common.dto.ApiResponse
import com.careercoach.llm.dto.ChatMessage
import com.careercoach.llm.dto.LlmResponse
import com.careercoach.llm.dto.MessageRole
import com.careercoach.llm.service.LlmService
import com.careercoach.llm.service.PromptTemplateService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

/**
 * LLM 테스트 컨트롤러
 * 개발 환경에서만 사용
 */
@Tag(name = "LLM Test", description = "LLM 통합 테스트 API")
@RestController
@RequestMapping("/api/v1/llm/test")
class LlmTestController(
    private val llmService: LlmService,
    private val promptTemplateService: PromptTemplateService
) {
    
    @Operation(summary = "LLM 상태 체크", description = "LLM 프로바이더 연결 상태를 확인합니다")
    @GetMapping("/health")
    fun checkHealth(): ApiResponse<Map<String, Any>> {
        val health = llmService.checkAllProvidersHealth()
        val providers = llmService.getAvailableProviders()
        
        return ApiResponse.success(
            mapOf(
                "available_providers" to providers,
                "health_status" to health
            )
        )
    }
    
    @Operation(summary = "텍스트 생성 테스트", description = "간단한 텍스트 생성을 테스트합니다")
    @PostMapping("/generate")
    fun generateText(
        @RequestBody request: GenerateTextRequest
    ): ApiResponse<LlmResponse> {
        val response = llmService.generateText(
            prompt = request.prompt,
            providerName = request.provider,
            maxTokens = request.maxTokens ?: 500,
            temperature = request.temperature ?: 0.7
        )
        
        return ApiResponse.success(response)
    }
    
    @Operation(summary = "구조화된 응답 생성", description = "JSON 형식의 구조화된 응답을 생성합니다")
    @PostMapping("/generate-structured")
    fun generateStructured(
        @RequestBody request: GenerateStructuredRequest
    ): ApiResponse<LlmResponse> {
        val response = llmService.generateStructuredResponse(
            prompt = request.prompt,
            responseSchema = request.schema,
            providerName = request.provider,
            maxTokens = request.maxTokens ?: 1000,
            temperature = request.temperature ?: 0.5
        )
        
        return ApiResponse.success(response)
    }
    
    @Operation(summary = "채팅 응답 생성", description = "대화 컨텍스트를 유지하며 응답을 생성합니다")
    @PostMapping("/chat")
    fun generateChat(
        @RequestBody request: ChatRequest
    ): ApiResponse<LlmResponse> {
        val messages = request.messages.map { 
            ChatMessage(
                role = MessageRole.valueOf(it.role.uppercase()),
                content = it.content
            )
        }
        
        val response = llmService.generateChatResponse(
            messages = messages,
            providerName = request.provider,
            maxTokens = request.maxTokens ?: 1000,
            temperature = request.temperature ?: 0.7
        )
        
        return ApiResponse.success(response)
    }
    
    @Operation(summary = "템플릿 목록 조회", description = "사용 가능한 프롬프트 템플릿 목록을 조회합니다")
    @GetMapping("/templates")
    fun getTemplates(): ApiResponse<List<TemplateInfo>> {
        val templates = promptTemplateService.getAllTemplates().map {
            TemplateInfo(
                name = it.name,
                description = it.description ?: "",
                variables = it.variables
            )
        }
        
        return ApiResponse.success(templates)
    }
    
    @Operation(summary = "템플릿 기반 생성", description = "프롬프트 템플릿을 사용하여 텍스트를 생성합니다")
    @PostMapping("/generate-from-template")
    fun generateFromTemplate(
        @RequestBody request: TemplateGenerateRequest
    ): ApiResponse<LlmResponse> {
        val response = llmService.generateFromTemplate(
            templateName = request.templateName,
            params = request.params,
            providerName = request.provider,
            maxTokens = request.maxTokens ?: 2000,
            temperature = request.temperature ?: 0.7
        )
        
        return ApiResponse.success(response)
    }
}

// Request DTOs
data class GenerateTextRequest(
    val prompt: String,
    val provider: String? = null,
    val maxTokens: Int? = null,
    val temperature: Double? = null
)

data class GenerateStructuredRequest(
    val prompt: String,
    val schema: Map<String, Any>,
    val provider: String? = null,
    val maxTokens: Int? = null,
    val temperature: Double? = null
)

data class ChatRequest(
    val messages: List<ChatMessageRequest>,
    val provider: String? = null,
    val maxTokens: Int? = null,
    val temperature: Double? = null
)

data class ChatMessageRequest(
    val role: String,
    val content: String
)

data class TemplateGenerateRequest(
    val templateName: String,
    val params: Map<String, Any>,
    val provider: String? = null,
    val maxTokens: Int? = null,
    val temperature: Double? = null
)

data class TemplateInfo(
    val name: String,
    val description: String,
    val variables: List<String>
)