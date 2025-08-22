package com.careercoach.monitoring.service

import io.micrometer.core.instrument.*
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Service
class MetricsCollectorService(
    private val meterRegistry: MeterRegistry
) {
    
    // API Metrics
    private val apiRequestCounter = Counter.builder("api.requests.total")
        .description("Total number of API requests")
        .register(meterRegistry)
    
    private val apiErrorCounter = Counter.builder("api.errors.total")
        .description("Total number of API errors")
        .register(meterRegistry)
    
    private val apiResponseTimer = Timer.builder("api.response.time")
        .description("API response time")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(meterRegistry)
    
    // LLM Metrics
    private val llmRequestCounter = Counter.builder("llm.requests.total")
        .description("Total number of LLM API calls")
        .register(meterRegistry)
    
    private val llmTokenCounter = Counter.builder("llm.tokens.total")
        .description("Total tokens used in LLM calls")
        .register(meterRegistry)
    
    private val llmResponseTimer = Timer.builder("llm.response.time")
        .description("LLM API response time")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(meterRegistry)
    
    private val llmCostGauge = meterRegistry.gauge("llm.cost.estimate", AtomicLong(0))
    
    // Cache Metrics
    private val cacheHitCounter = Counter.builder("cache.hits.total")
        .description("Total cache hits")
        .register(meterRegistry)
    
    private val cacheMissCounter = Counter.builder("cache.misses.total")
        .description("Total cache misses")
        .register(meterRegistry)
    
    // Profile Metrics
    private val profileCounter = Counter.builder("profiles.created.total")
        .description("Total profiles created")
        .register(meterRegistry)
    
    private val activeProfilesGauge = meterRegistry.gauge("profiles.active", AtomicInteger(0))
    
    // Agent Metrics
    private val agentExecutionCounter = ConcurrentHashMap<String, Counter>()
    private val agentExecutionTimer = ConcurrentHashMap<String, Timer>()
    
    // Real-time metrics storage
    private val realtimeMetrics = RealtimeMetrics()
    
    fun recordApiRequest(endpoint: String, method: String, status: Int, duration: Long) {
        apiRequestCounter.increment()
        apiResponseTimer.record(Duration.ofMillis(duration))
        
        if (status >= 400) {
            apiErrorCounter.increment()
        }
        
        meterRegistry.counter("api.requests",
            "endpoint", endpoint,
            "method", method,
            "status", status.toString()
        ).increment()
        
        realtimeMetrics.updateApiMetrics(endpoint, method, status, duration)
    }
    
    fun recordLlmRequest(provider: String, model: String, tokens: Int, duration: Long, cost: Double) {
        llmRequestCounter.increment()
        llmTokenCounter.increment(tokens.toDouble())
        llmResponseTimer.record(Duration.ofMillis(duration))
        
        meterRegistry.counter("llm.requests",
            "provider", provider,
            "model", model
        ).increment()
        
        meterRegistry.counter("llm.tokens",
            "provider", provider,
            "model", model
        ).increment(tokens.toDouble())
        
        // Update cost estimate
        val currentCost = llmCostGauge?.get() ?: 0L
        llmCostGauge?.set((currentCost + (cost * 1000).toLong()))
        
        realtimeMetrics.updateLlmMetrics(provider, model, tokens, duration, cost)
    }
    
    fun recordCacheHit(cacheType: String) {
        cacheHitCounter.increment()
        meterRegistry.counter("cache.hits", "type", cacheType).increment()
        realtimeMetrics.updateCacheMetrics(true, cacheType)
    }
    
    fun recordCacheMiss(cacheType: String) {
        cacheMissCounter.increment()
        meterRegistry.counter("cache.misses", "type", cacheType).increment()
        realtimeMetrics.updateCacheMetrics(false, cacheType)
    }
    
    fun recordProfileCreated() {
        profileCounter.increment()
        activeProfilesGauge?.incrementAndGet()
        realtimeMetrics.incrementProfileCount()
    }
    
    fun recordAgentExecution(agentName: String, duration: Long, success: Boolean) {
        val counter = agentExecutionCounter.computeIfAbsent(agentName) {
            Counter.builder("agent.executions")
                .tag("agent", agentName)
                .tag("status", if (success) "success" else "failure")
                .register(meterRegistry)
        }
        counter.increment()
        
        val timer = agentExecutionTimer.computeIfAbsent(agentName) {
            Timer.builder("agent.execution.time")
                .tag("agent", agentName)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry)
        }
        timer.record(Duration.ofMillis(duration))
        
        realtimeMetrics.updateAgentMetrics(agentName, duration, success)
    }
    
    fun getRealtimeMetrics(): RealtimeMetricsSnapshot {
        return realtimeMetrics.getSnapshot()
    }
    
    fun getDashboardMetrics(): DashboardMetrics {
        val apiMetrics = ApiMetrics(
            totalRequests = apiRequestCounter.count().toLong(),
            totalErrors = apiErrorCounter.count().toLong(),
            averageResponseTime = apiResponseTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS),
            p95ResponseTime = apiResponseTimer.percentile(0.95, java.util.concurrent.TimeUnit.MILLISECONDS),
            p99ResponseTime = apiResponseTimer.percentile(0.99, java.util.concurrent.TimeUnit.MILLISECONDS)
        )
        
        val llmMetrics = LlmMetrics(
            totalRequests = llmRequestCounter.count().toLong(),
            totalTokens = llmTokenCounter.count().toLong(),
            averageResponseTime = llmResponseTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS),
            estimatedCost = (llmCostGauge?.get() ?: 0L) / 1000.0
        )
        
        val cacheMetrics = CacheMetrics(
            totalHits = cacheHitCounter.count().toLong(),
            totalMisses = cacheMissCounter.count().toLong(),
            hitRate = calculateHitRate()
        )
        
        val systemMetrics = SystemMetrics(
            cpuUsage = getCpuUsage(),
            memoryUsage = getMemoryUsage(),
            activeThreads = Thread.activeCount(),
            uptime = getUptime()
        )
        
        return DashboardMetrics(
            api = apiMetrics,
            llm = llmMetrics,
            cache = cacheMetrics,
            system = systemMetrics,
            realtime = getRealtimeMetrics()
        )
    }
    
    private fun calculateHitRate(): Double {
        val hits = cacheHitCounter.count()
        val misses = cacheMissCounter.count()
        val total = hits + misses
        return if (total > 0) hits / total else 0.0
    }
    
    private fun getCpuUsage(): Double {
        val runtime = Runtime.getRuntime()
        val processors = runtime.availableProcessors()
        return (runtime.totalMemory() - runtime.freeMemory()).toDouble() / runtime.maxMemory() * 100
    }
    
    private fun getMemoryUsage(): MemoryUsage {
        val runtime = Runtime.getRuntime()
        return MemoryUsage(
            used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
            total = runtime.totalMemory() / 1024 / 1024,
            max = runtime.maxMemory() / 1024 / 1024
        )
    }
    
    private fun getUptime(): Long {
        val runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean()
        return runtimeMxBean.uptime / 1000 // Convert to seconds
    }
}

// Data classes for metrics
data class DashboardMetrics(
    val api: ApiMetrics,
    val llm: LlmMetrics,
    val cache: CacheMetrics,
    val system: SystemMetrics,
    val realtime: RealtimeMetricsSnapshot
)

data class ApiMetrics(
    val totalRequests: Long,
    val totalErrors: Long,
    val averageResponseTime: Double,
    val p95ResponseTime: Double,
    val p99ResponseTime: Double
)

data class LlmMetrics(
    val totalRequests: Long,
    val totalTokens: Long,
    val averageResponseTime: Double,
    val estimatedCost: Double
)

data class CacheMetrics(
    val totalHits: Long,
    val totalMisses: Long,
    val hitRate: Double
)

data class SystemMetrics(
    val cpuUsage: Double,
    val memoryUsage: MemoryUsage,
    val activeThreads: Int,
    val uptime: Long
)

data class MemoryUsage(
    val used: Long,
    val total: Long,
    val max: Long
)

// Real-time metrics tracking
class RealtimeMetrics {
    private val apiMetricsBuffer = CircularBuffer<ApiRequestMetric>(100)
    private val llmMetricsBuffer = CircularBuffer<LlmRequestMetric>(50)
    private val cacheMetricsBuffer = CircularBuffer<CacheAccessMetric>(100)
    private val agentMetricsBuffer = CircularBuffer<AgentExecutionMetric>(50)
    private val profileCount = AtomicInteger(0)
    
    fun updateApiMetrics(endpoint: String, method: String, status: Int, duration: Long) {
        apiMetricsBuffer.add(ApiRequestMetric(
            timestamp = System.currentTimeMillis(),
            endpoint = endpoint,
            method = method,
            status = status,
            duration = duration
        ))
    }
    
    fun updateLlmMetrics(provider: String, model: String, tokens: Int, duration: Long, cost: Double) {
        llmMetricsBuffer.add(LlmRequestMetric(
            timestamp = System.currentTimeMillis(),
            provider = provider,
            model = model,
            tokens = tokens,
            duration = duration,
            cost = cost
        ))
    }
    
    fun updateCacheMetrics(hit: Boolean, cacheType: String) {
        cacheMetricsBuffer.add(CacheAccessMetric(
            timestamp = System.currentTimeMillis(),
            hit = hit,
            cacheType = cacheType
        ))
    }
    
    fun updateAgentMetrics(agentName: String, duration: Long, success: Boolean) {
        agentMetricsBuffer.add(AgentExecutionMetric(
            timestamp = System.currentTimeMillis(),
            agentName = agentName,
            duration = duration,
            success = success
        ))
    }
    
    fun incrementProfileCount() {
        profileCount.incrementAndGet()
    }
    
    fun getSnapshot(): RealtimeMetricsSnapshot {
        return RealtimeMetricsSnapshot(
            recentApiRequests = apiMetricsBuffer.getAll(),
            recentLlmRequests = llmMetricsBuffer.getAll(),
            recentCacheAccesses = cacheMetricsBuffer.getAll(),
            recentAgentExecutions = agentMetricsBuffer.getAll(),
            totalProfiles = profileCount.get()
        )
    }
}

data class RealtimeMetricsSnapshot(
    val recentApiRequests: List<ApiRequestMetric>,
    val recentLlmRequests: List<LlmRequestMetric>,
    val recentCacheAccesses: List<CacheAccessMetric>,
    val recentAgentExecutions: List<AgentExecutionMetric>,
    val totalProfiles: Int
)

data class ApiRequestMetric(
    val timestamp: Long,
    val endpoint: String,
    val method: String,
    val status: Int,
    val duration: Long
)

data class LlmRequestMetric(
    val timestamp: Long,
    val provider: String,
    val model: String,
    val tokens: Int,
    val duration: Long,
    val cost: Double
)

data class CacheAccessMetric(
    val timestamp: Long,
    val hit: Boolean,
    val cacheType: String
)

data class AgentExecutionMetric(
    val timestamp: Long,
    val agentName: String,
    val duration: Long,
    val success: Boolean
)

// Circular buffer for storing recent metrics
class CircularBuffer<T>(private val capacity: Int) {
    private val buffer = mutableListOf<T>()
    
    @Synchronized
    fun add(item: T) {
        if (buffer.size >= capacity) {
            buffer.removeAt(0)
        }
        buffer.add(item)
    }
    
    @Synchronized
    fun getAll(): List<T> {
        return buffer.toList()
    }
}