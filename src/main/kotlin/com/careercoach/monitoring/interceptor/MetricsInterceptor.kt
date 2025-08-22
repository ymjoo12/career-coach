package com.careercoach.monitoring.interceptor

import com.careercoach.monitoring.service.MetricsCollectorService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class MetricsInterceptor(
    private val metricsCollector: MetricsCollectorService
) : HandlerInterceptor {
    
    companion object {
        const val START_TIME_ATTRIBUTE = "startTime"
    }
    
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis())
        return true
    }
    
    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        val startTime = request.getAttribute(START_TIME_ATTRIBUTE) as? Long ?: return
        val duration = System.currentTimeMillis() - startTime
        
        val endpoint = request.requestURI
        val method = request.method
        val status = response.status
        
        // Skip metrics for actuator endpoints
        if (!endpoint.startsWith("/actuator")) {
            metricsCollector.recordApiRequest(endpoint, method, status, duration)
        }
    }
}