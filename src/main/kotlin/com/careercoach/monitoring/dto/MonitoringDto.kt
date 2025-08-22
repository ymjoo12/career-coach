package com.careercoach.monitoring.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "대시보드 응답")
data class DashboardResponse(
    @Schema(description = "타임스탬프")
    val timestamp: Long,
    
    @Schema(description = "API 메트릭")
    val api: ApiMetricsDto,
    
    @Schema(description = "LLM 메트릭")
    val llm: LlmMetricsDto,
    
    @Schema(description = "캐시 메트릭")
    val cache: CacheMetricsDto,
    
    @Schema(description = "시스템 메트릭")
    val system: SystemMetricsDto,
    
    @Schema(description = "프로필 메트릭")
    val profiles: ProfileMetricsDto,
    
    @Schema(description = "에이전트 메트릭")
    val agents: List<AgentMetricsDto>
)

@Schema(description = "API 메트릭")
data class ApiMetricsDto(
    @Schema(description = "총 요청 수")
    val totalRequests: Long,
    
    @Schema(description = "총 에러 수")
    val totalErrors: Long,
    
    @Schema(description = "에러율 (%)")
    val errorRate: Double,
    
    @Schema(description = "평균 응답 시간 (ms)")
    val averageResponseTime: Double,
    
    @Schema(description = "95 백분위 응답 시간 (ms)")
    val p95ResponseTime: Double,
    
    @Schema(description = "99 백분위 응답 시간 (ms)")
    val p99ResponseTime: Double,
    
    @Schema(description = "분당 요청 수")
    val requestsPerMinute: Double
)

@Schema(description = "LLM 메트릭")
data class LlmMetricsDto(
    @Schema(description = "총 요청 수")
    val totalRequests: Long,
    
    @Schema(description = "총 토큰 수")
    val totalTokens: Long,
    
    @Schema(description = "평균 응답 시간 (ms)")
    val averageResponseTime: Double,
    
    @Schema(description = "예상 비용 ($)")
    val estimatedCost: Double,
    
    @Schema(description = "요청당 평균 토큰")
    val tokensPerRequest: Double
)

@Schema(description = "캐시 메트릭")
data class CacheMetricsDto(
    @Schema(description = "총 히트 수")
    val totalHits: Long,
    
    @Schema(description = "총 미스 수")
    val totalMisses: Long,
    
    @Schema(description = "히트율 (%)")
    val hitRate: Double,
    
    @Schema(description = "절약된 API 호출 수")
    val savedApiCalls: Long
)

@Schema(description = "시스템 메트릭")
data class SystemMetricsDto(
    @Schema(description = "CPU 사용률 (%)")
    val cpuUsage: Double,
    
    @Schema(description = "사용 메모리 (MB)")
    val memoryUsed: Long,
    
    @Schema(description = "총 메모리 (MB)")
    val memoryTotal: Long,
    
    @Schema(description = "최대 메모리 (MB)")
    val memoryMax: Long,
    
    @Schema(description = "활성 스레드 수")
    val activeThreads: Int,
    
    @Schema(description = "업타임")
    val uptime: String
)

@Schema(description = "프로필 메트릭")
data class ProfileMetricsDto(
    @Schema(description = "총 프로필 수")
    val totalProfiles: Int,
    
    @Schema(description = "오늘 생성된 프로필 수")
    val profilesCreatedToday: Int
)

@Schema(description = "에이전트 메트릭")
data class AgentMetricsDto(
    @Schema(description = "에이전트 이름")
    val name: String,
    
    @Schema(description = "총 실행 수")
    val totalExecutions: Int,
    
    @Schema(description = "성공률 (%)")
    val successRate: Double,
    
    @Schema(description = "평균 실행 시간 (ms)")
    val averageDuration: Double
)

@Schema(description = "실시간 메트릭")
data class RealtimeMetricsDto(
    @Schema(description = "타임스탬프")
    val timestamp: Long,
    
    @Schema(description = "최근 API 요청")
    val recentApiRequests: List<ApiRequestDto>,
    
    @Schema(description = "최근 LLM 요청")
    val recentLlmRequests: List<LlmRequestDto>,
    
    @Schema(description = "최근 캐시 히트율 (%)")
    val cacheHitRate: Double,
    
    @Schema(description = "활성 에이전트 수")
    val activeAgents: Int
)

@Schema(description = "API 요청 정보")
data class ApiRequestDto(
    @Schema(description = "타임스탬프")
    val timestamp: Long,
    
    @Schema(description = "엔드포인트")
    val endpoint: String,
    
    @Schema(description = "HTTP 메소드")
    val method: String,
    
    @Schema(description = "상태 코드")
    val status: Int,
    
    @Schema(description = "응답 시간 (ms)")
    val duration: Long,
    
    @Schema(description = "성공 여부")
    val success: Boolean
)

@Schema(description = "LLM 요청 정보")
data class LlmRequestDto(
    @Schema(description = "타임스탬프")
    val timestamp: Long,
    
    @Schema(description = "프로바이더")
    val provider: String,
    
    @Schema(description = "모델")
    val model: String,
    
    @Schema(description = "토큰 수")
    val tokens: Int,
    
    @Schema(description = "응답 시간 (ms)")
    val duration: Long,
    
    @Schema(description = "비용 ($)")
    val cost: Double
)

@Schema(description = "시스템 헬스")
data class SystemHealthDto(
    @Schema(description = "전체 상태")
    val status: String,
    
    @Schema(description = "컴포넌트 상태")
    val components: List<ComponentHealthDto>,
    
    @Schema(description = "타임스탬프")
    val timestamp: Long
)

@Schema(description = "컴포넌트 헬스")
data class ComponentHealthDto(
    @Schema(description = "컴포넌트 이름")
    val name: String,
    
    @Schema(description = "상태")
    val status: String,
    
    @Schema(description = "메시지")
    val message: String?,
    
    @Schema(description = "응답 시간 (ms)")
    val responseTime: Long?
)

@Schema(description = "시스템 알림")
data class AlertDto(
    @Schema(description = "알림 레벨", example = "INFO, WARNING, ERROR")
    val level: String,
    
    @Schema(description = "알림 타입")
    val type: String,
    
    @Schema(description = "알림 메시지")
    val message: String,
    
    @Schema(description = "타임스탬프")
    val timestamp: Long
)

@Schema(description = "커스텀 메트릭 요청")
data class CustomMetricRequest(
    @Schema(description = "메트릭 이름")
    val name: String,
    
    @Schema(description = "메트릭 값")
    val value: Double,
    
    @Schema(description = "태그")
    val tags: Map<String, String> = emptyMap(),
    
    @Schema(description = "타임스탬프")
    val timestamp: Long = System.currentTimeMillis()
)