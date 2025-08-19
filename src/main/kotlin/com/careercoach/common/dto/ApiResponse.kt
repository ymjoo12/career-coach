package com.careercoach.common.dto

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorResponse? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data
            )
        }
        
        fun <T> success(): ApiResponse<T> {
            return ApiResponse(
                success = true
            )
        }
        
        fun <T> error(status: HttpStatus, message: String, details: Map<String, Any>? = null): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorResponse(
                    status = status.value(),
                    message = message,
                    details = details
                )
            )
        }
    }
}

data class ErrorResponse(
    val status: Int,
    val message: String,
    val details: Map<String, Any>? = null
)