package com.careercoach.common.exception

import org.springframework.http.HttpStatus

open class BusinessException(
    val status: HttpStatus,
    override val message: String,
    val details: Map<String, Any>? = null
) : RuntimeException(message)

class ResourceNotFoundException(
    message: String,
    details: Map<String, Any>? = null
) : BusinessException(HttpStatus.NOT_FOUND, message, details)

class ValidationException(
    message: String,
    details: Map<String, Any>? = null
) : BusinessException(HttpStatus.BAD_REQUEST, message, details)

class DuplicateResourceException(
    message: String,
    details: Map<String, Any>? = null
) : BusinessException(HttpStatus.CONFLICT, message, details)

class ExternalApiException(
    message: String,
    details: Map<String, Any>? = null
) : BusinessException(HttpStatus.SERVICE_UNAVAILABLE, message, details)

class UnauthorizedException(
    message: String = "Unauthorized access",
    details: Map<String, Any>? = null
) : BusinessException(HttpStatus.UNAUTHORIZED, message, details)

class ForbiddenException(
    message: String = "Access forbidden",
    details: Map<String, Any>? = null
) : BusinessException(HttpStatus.FORBIDDEN, message, details)