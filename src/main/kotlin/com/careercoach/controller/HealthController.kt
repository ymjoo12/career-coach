package com.careercoach.controller

import com.careercoach.common.dto.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Health", description = "헬스체크 API")
@RestController
@RequestMapping("/api/health")
class HealthController {

    @Operation(summary = "헬스체크", description = "서비스 상태를 확인합니다")
    @GetMapping
    fun health(): ApiResponse<Map<String, String>> {
        val data = mapOf(
            "status" to "UP",
            "service" to "Career Coach API",
            "version" to "0.0.1-SNAPSHOT"
        )
        return ApiResponse.success(data)
    }
}