package com.careercoach.llm.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * LLM 설정 활성화
 */
@Configuration
@EnableConfigurationProperties(LlmProperties::class)
class LlmConfiguration

/**
 * LLM 설정 프로퍼티
 */
@ConfigurationProperties(prefix = "llm")
data class LlmProperties(
    var defaultProvider: String = "gemini",
    var gemini: GeminiProperties = GeminiProperties(),
    var cache: CacheProperties = CacheProperties(),
    var retry: RetryProperties = RetryProperties()
)

/**
 * Gemini 프로바이더 설정
 */
data class GeminiProperties(
    var apiKey: String = "",
    var model: String = "gemini-1.5-flash",
    var endpoint: String = "https://generativelanguage.googleapis.com/v1beta",
    var timeout: Long = 30000,
    var maxRetries: Int = 3
)

/**
 * 캐시 설정
 */
data class CacheProperties(
    var enabled: Boolean = true,
    var ttl: Long = 86400, // 24 hours in seconds
    var maxSize: Int = 1000,
    var similarity: SimilarityProperties = SimilarityProperties()
)

/**
 * 유사도 기반 캐싱 설정
 */
data class SimilarityProperties(
    var enabled: Boolean = true,
    var threshold: Double = 0.85,
    var partialThreshold: Double = 0.70
)

/**
 * 재시도 설정
 */
data class RetryProperties(
    var maxAttempts: Int = 3,
    var delay: Long = 1000,
    var maxDelay: Long = 10000,
    var multiplier: Double = 2.0
)