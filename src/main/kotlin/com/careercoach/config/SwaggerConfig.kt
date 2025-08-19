package com.careercoach.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "Career Coach API",
        version = "v1.0.0",
        description = "이력서 기반 개인 맞춤형 커리어 코치 챗봇 API",
        contact = Contact(
            name = "Career Coach Team",
            email = "support@careercoach.com"
        ),
        license = License(
            name = "Private License"
        )
    ),
    servers = [
        Server(url = "http://localhost:8090", description = "Local Development Server"),
        Server(url = "https://api.careercoach.com", description = "Production Server")
    ]
)
class SwaggerConfig {
    
    @Bean
    fun customOpenAPI(): OpenAPI {
        val securitySchemeName = "bearerAuth"
        
        return OpenAPI()
            .components(
                Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT Bearer Token for API Authentication")
                    )
            )
            .addSecurityItem(
                SecurityRequirement()
                    .addList(securitySchemeName)
            )
    }
}