package com.careercoach.monitoring.service

import com.careercoach.domain.profile.repository.ProfileRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class SystemHealthService(
    private val jdbcTemplate: JdbcTemplate,
    private val profileRepository: ProfileRepository,
    private val webClient: WebClient
) {
    
    fun checkHealth(): SystemHealth {
        val components = mutableMapOf<String, ComponentStatus>()
        
        // Check Database
        components["database"] = checkDatabase()
        
        // Check LLM Service
        components["llm"] = checkLlmService()
        
        // Check Cache
        components["cache"] = checkCacheService()
        
        // Check JVM
        components["jvm"] = checkJvmHealth()
        
        // Determine overall status
        val overallStatus = when {
            components.values.any { it.status == "DOWN" } -> "DOWN"
            components.values.any { it.status == "DEGRADED" } -> "DEGRADED"
            else -> "UP"
        }
        
        return SystemHealth(
            status = overallStatus,
            components = components
        )
    }
    
    private fun checkDatabase(): ComponentStatus {
        return try {
            val start = System.currentTimeMillis()
            jdbcTemplate.queryForObject("SELECT 1", Int::class.java)
            val duration = System.currentTimeMillis() - start
            
            val profileCount = profileRepository.count()
            
            ComponentStatus(
                status = if (duration < 100) "UP" else "DEGRADED",
                message = "Database connected. Profiles: $profileCount",
                responseTime = duration
            )
        } catch (e: Exception) {
            ComponentStatus(
                status = "DOWN",
                message = "Database connection failed: ${e.message}",
                responseTime = null
            )
        }
    }
    
    private fun checkLlmService(): ComponentStatus {
        return try {
            // Check if LLM service is configured
            val apiKey = System.getenv("GEMINI_API_KEY")
            if (apiKey.isNullOrBlank()) {
                ComponentStatus(
                    status = "DEGRADED",
                    message = "LLM API key not configured",
                    responseTime = null
                )
            } else {
                ComponentStatus(
                    status = "UP",
                    message = "LLM service configured",
                    responseTime = null
                )
            }
        } catch (e: Exception) {
            ComponentStatus(
                status = "DOWN",
                message = "LLM service check failed: ${e.message}",
                responseTime = null
            )
        }
    }
    
    private fun checkCacheService(): ComponentStatus {
        return try {
            // Simple cache health check
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val memoryUsagePercent = (usedMemory.toDouble() / maxMemory) * 100
            
            ComponentStatus(
                status = when {
                    memoryUsagePercent > 90 -> "DEGRADED"
                    else -> "UP"
                },
                message = "Cache memory usage: ${String.format("%.2f", memoryUsagePercent)}%",
                responseTime = null
            )
        } catch (e: Exception) {
            ComponentStatus(
                status = "DOWN",
                message = "Cache check failed: ${e.message}",
                responseTime = null
            )
        }
    }
    
    private fun checkJvmHealth(): ComponentStatus {
        return try {
            val runtime = Runtime.getRuntime()
            val processors = runtime.availableProcessors()
            val freeMemory = runtime.freeMemory() / 1024 / 1024
            val totalMemory = runtime.totalMemory() / 1024 / 1024
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            
            ComponentStatus(
                status = "UP",
                message = "Processors: $processors, Memory (MB): $freeMemory/$totalMemory/$maxMemory (free/total/max)",
                responseTime = null
            )
        } catch (e: Exception) {
            ComponentStatus(
                status = "DEGRADED",
                message = "JVM health check partial: ${e.message}",
                responseTime = null
            )
        }
    }
    
    fun performHealthCheck(url: String): Mono<Boolean> {
        return webClient.get()
            .uri(url)
            .retrieve()
            .toBodilessEntity()
            .timeout(Duration.ofSeconds(5))
            .map { true }
            .onErrorReturn(false)
    }
}

data class SystemHealth(
    val status: String,
    val components: Map<String, ComponentStatus>
)

data class ComponentStatus(
    val status: String,
    val message: String?,
    val responseTime: Long?
)