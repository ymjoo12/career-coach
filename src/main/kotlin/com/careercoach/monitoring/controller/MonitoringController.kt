package com.careercoach.monitoring.controller

import com.careercoach.common.dto.ApiResponse
import com.careercoach.monitoring.dto.*
import com.careercoach.monitoring.service.MetricsCollectorService
import com.careercoach.monitoring.service.SystemHealthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/monitoring")
@Tag(name = "Monitoring", description = "시스템 모니터링 및 메트릭 API")
class MonitoringController(
    private val metricsCollector: MetricsCollectorService,
    private val systemHealthService: SystemHealthService
) {
    
    @GetMapping("/dashboard")
    @Operation(summary = "대시보드 메트릭 조회", description = "시스템 전반의 메트릭 데이터 조회")
    fun getDashboardMetrics(): ApiResponse<DashboardResponse> {
        val metrics = metricsCollector.getDashboardMetrics()
        
        return ApiResponse.success(
            DashboardResponse(
                timestamp = System.currentTimeMillis(),
                api = ApiMetricsDto(
                    totalRequests = metrics.api.totalRequests,
                    totalErrors = metrics.api.totalErrors,
                    errorRate = if (metrics.api.totalRequests > 0) 
                        metrics.api.totalErrors.toDouble() / metrics.api.totalRequests * 100 else 0.0,
                    averageResponseTime = metrics.api.averageResponseTime,
                    p95ResponseTime = metrics.api.p95ResponseTime,
                    p99ResponseTime = metrics.api.p99ResponseTime,
                    requestsPerMinute = calculateRequestsPerMinute(metrics.realtime.recentApiRequests)
                ),
                llm = LlmMetricsDto(
                    totalRequests = metrics.llm.totalRequests,
                    totalTokens = metrics.llm.totalTokens,
                    averageResponseTime = metrics.llm.averageResponseTime,
                    estimatedCost = metrics.llm.estimatedCost,
                    tokensPerRequest = if (metrics.llm.totalRequests > 0)
                        metrics.llm.totalTokens.toDouble() / metrics.llm.totalRequests else 0.0
                ),
                cache = CacheMetricsDto(
                    totalHits = metrics.cache.totalHits,
                    totalMisses = metrics.cache.totalMisses,
                    hitRate = metrics.cache.hitRate * 100,
                    savedApiCalls = estimateSavedApiCalls(metrics.cache)
                ),
                system = SystemMetricsDto(
                    cpuUsage = metrics.system.cpuUsage,
                    memoryUsed = metrics.system.memoryUsage.used,
                    memoryTotal = metrics.system.memoryUsage.total,
                    memoryMax = metrics.system.memoryUsage.max,
                    activeThreads = metrics.system.activeThreads,
                    uptime = formatUptime(metrics.system.uptime)
                ),
                profiles = ProfileMetricsDto(
                    totalProfiles = metrics.realtime.totalProfiles,
                    profilesCreatedToday = countTodayProfiles(metrics.realtime)
                ),
                agents = extractAgentMetrics(metrics.realtime.recentAgentExecutions)
            )
        )
    }
    
    @GetMapping("/realtime")
    @Operation(summary = "실시간 메트릭 조회", description = "최근 활동에 대한 실시간 메트릭 조회")
    fun getRealtimeMetrics(): ApiResponse<RealtimeMetricsDto> {
        val snapshot = metricsCollector.getRealtimeMetrics()
        
        return ApiResponse.success(
            RealtimeMetricsDto(
                timestamp = System.currentTimeMillis(),
                recentApiRequests = snapshot.recentApiRequests.takeLast(20).map { metric ->
                    ApiRequestDto(
                        timestamp = metric.timestamp,
                        endpoint = metric.endpoint,
                        method = metric.method,
                        status = metric.status,
                        duration = metric.duration,
                        success = metric.status < 400
                    )
                },
                recentLlmRequests = snapshot.recentLlmRequests.takeLast(10).map { metric ->
                    LlmRequestDto(
                        timestamp = metric.timestamp,
                        provider = metric.provider,
                        model = metric.model,
                        tokens = metric.tokens,
                        duration = metric.duration,
                        cost = metric.cost
                    )
                },
                cacheHitRate = calculateRecentCacheHitRate(snapshot.recentCacheAccesses),
                activeAgents = countActiveAgents(snapshot.recentAgentExecutions)
            )
        )
    }
    
    @GetMapping("/health")
    @Operation(summary = "시스템 헬스 체크", description = "시스템 컴포넌트 상태 확인")
    fun getSystemHealth(): ApiResponse<SystemHealthDto> {
        val health = systemHealthService.checkHealth()
        
        return ApiResponse.success(
            SystemHealthDto(
                status = health.status,
                components = health.components.map { (name, status) ->
                    ComponentHealthDto(
                        name = name,
                        status = status.status,
                        message = status.message,
                        responseTime = status.responseTime
                    )
                },
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    @GetMapping("/alerts")
    @Operation(summary = "시스템 알림 조회", description = "활성 알림 및 경고 조회")
    fun getSystemAlerts(): ApiResponse<List<AlertDto>> {
        val health = systemHealthService.checkHealth()
        val metrics = metricsCollector.getDashboardMetrics()
        
        val alerts = mutableListOf<AlertDto>()
        
        // Check API error rate
        val errorRate = if (metrics.api.totalRequests > 0) 
            metrics.api.totalErrors.toDouble() / metrics.api.totalRequests * 100 else 0.0
        if (errorRate > 5.0) {
            alerts.add(AlertDto(
                level = if (errorRate > 10.0) "ERROR" else "WARNING",
                type = "API_ERROR_RATE",
                message = "API 에러율이 ${String.format("%.2f", errorRate)}% 입니다",
                timestamp = System.currentTimeMillis()
            ))
        }
        
        // Check cache hit rate
        if (metrics.cache.hitRate < 0.3 && metrics.cache.totalHits + metrics.cache.totalMisses > 100) {
            alerts.add(AlertDto(
                level = "WARNING",
                type = "LOW_CACHE_HIT_RATE",
                message = "캐시 적중률이 ${String.format("%.2f", metrics.cache.hitRate * 100)}%로 낮습니다",
                timestamp = System.currentTimeMillis()
            ))
        }
        
        // Check memory usage
        val memoryUsagePercent = metrics.system.memoryUsage.used.toDouble() / metrics.system.memoryUsage.max * 100
        if (memoryUsagePercent > 80) {
            alerts.add(AlertDto(
                level = if (memoryUsagePercent > 90) "ERROR" else "WARNING",
                type = "HIGH_MEMORY_USAGE",
                message = "메모리 사용률이 ${String.format("%.2f", memoryUsagePercent)}% 입니다",
                timestamp = System.currentTimeMillis()
            ))
        }
        
        // Check component health
        health.components.forEach { (name, status) ->
            if (status.status != "UP") {
                alerts.add(AlertDto(
                    level = if (status.status == "DOWN") "ERROR" else "WARNING",
                    type = "COMPONENT_HEALTH",
                    message = "$name 컴포넌트: ${status.message}",
                    timestamp = System.currentTimeMillis()
                ))
            }
        }
        
        return ApiResponse.success(alerts.sortedByDescending { it.timestamp })
    }
    
    @PostMapping("/metrics/custom")
    @Operation(summary = "커스텀 메트릭 기록", description = "애플리케이션 커스텀 메트릭 기록")
    fun recordCustomMetric(@RequestBody request: CustomMetricRequest): ApiResponse<String> {
        // Record custom metric logic here
        return ApiResponse.success("Custom metric recorded: ${request.name}")
    }
    
    private fun calculateRequestsPerMinute(recentRequests: List<com.careercoach.monitoring.service.ApiRequestMetric>): Double {
        if (recentRequests.isEmpty()) return 0.0
        
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 60000
        val requestsInLastMinute = recentRequests.count { it.timestamp >= oneMinuteAgo }
        
        return requestsInLastMinute.toDouble()
    }
    
    private fun estimateSavedApiCalls(cache: com.careercoach.monitoring.service.CacheMetrics): Long {
        // Estimate based on cache hits (each hit saves 1-2 API calls)
        return (cache.totalHits * 1.5).toLong()
    }
    
    private fun formatUptime(seconds: Long): String {
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        
        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
    
    private fun countTodayProfiles(realtime: com.careercoach.monitoring.service.RealtimeMetricsSnapshot): Int {
        // This would need actual implementation based on profile creation timestamps
        return realtime.totalProfiles / 10 // Placeholder
    }
    
    private fun extractAgentMetrics(executions: List<com.careercoach.monitoring.service.AgentExecutionMetric>): List<AgentMetricsDto> {
        return executions.groupBy { it.agentName }
            .map { (name, metrics) ->
                AgentMetricsDto(
                    name = name,
                    totalExecutions = metrics.size,
                    successRate = metrics.count { it.success }.toDouble() / metrics.size * 100,
                    averageDuration = metrics.map { it.duration }.average()
                )
            }
    }
    
    private fun calculateRecentCacheHitRate(accesses: List<com.careercoach.monitoring.service.CacheAccessMetric>): Double {
        if (accesses.isEmpty()) return 0.0
        val hits = accesses.count { it.hit }
        return hits.toDouble() / accesses.size * 100
    }
    
    private fun countActiveAgents(executions: List<com.careercoach.monitoring.service.AgentExecutionMetric>): Int {
        val fiveMinutesAgo = System.currentTimeMillis() - 300000
        return executions
            .filter { it.timestamp >= fiveMinutesAgo }
            .map { it.agentName }
            .distinct()
            .size
    }
}