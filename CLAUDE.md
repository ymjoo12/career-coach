# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Resume-based Personalized Career Coach Chatbot API for the Korean market. Generates personalized interview questions and learning paths based on resume information.

## Tech Stack

- **Backend**: Kotlin + Spring Boot 3.x
- **Database**: PostgreSQL (Docker containers)
- **LLM Integration**: Google Gemini API (extensible Provider pattern)
- **Build**: Gradle
- **Testing**: JUnit 5 + MockK + TestContainers

## Common Commands

### Build & Run
```bash
# Build project
./gradlew build

# Run application
./gradlew bootRun

# Start database (Docker)
docker-compose up -d postgres

# Run database migrations
./gradlew flywayMigrate
```

### Testing
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.careercoach.*TestClassName"

# Run with coverage
./gradlew test jacocoTestReport
```

### Development
```bash
# Clean build
./gradlew clean build

# Format code
./gradlew ktlintFormat

# Check code style
./gradlew ktlintCheck
```

## Architecture

### Layered Structure
```
Controller → Service → Repository → External APIs
```

### Package Organization
```
com.careercoach/
├── config/         # Spring configuration, security, WebMVC
├── controller/     # REST endpoints with validation
├── service/        # Business logic with transaction management
├── domain/         # JPA entities
├── repository/     # Spring Data JPA repositories
├── dto/           # Request/Response DTOs with validation
├── external/      # LLM provider integration (Provider pattern)
└── common/        # Shared utilities and exceptions
```

### Key Patterns

1. **LLM Provider Pattern**: External services use Provider interface for extensibility
   - `LLMProvider` interface in `external/llm/`
   - Implementations: `GeminiProvider`, future providers

2. **Smart Caching Strategy**: Profile similarity-based caching (Phase 2)
   - Cache similar profile questions
   - Reduce LLM API calls by 40-60%

3. **Two-Stage Question Generation**:
   - Stage 1: Generate base questions from profile
   - Stage 2: Enhance with position-specific context

## API Structure

### Core Endpoints
- `POST /api/v1/profiles` - Create user profile
- `GET /api/v1/profiles/{id}/questions` - Generate interview questions
- `POST /api/v1/profiles/{id}/questions/followup` - Generate follow-up questions
- `GET /api/v1/profiles/{id}/learning-paths` - Get learning recommendations

## Development Phases

**Current Phase**: Planning → Implementation (Phase 1 MVP)

### Phase 1 Tasks (TASK-001 to TASK-008)
- Basic profile CRUD operations
- Two-stage question generation
- Simple caching mechanism
- Learning path generation

### Phase 2 Tasks (TASK-009 to TASK-013)
- Multi-Agent system implementation
- Advanced similarity-based caching
- Context intelligence layer

### Phase 3 Tasks (TASK-014 to TASK-017)
- Real-time interview simulation
- Prediction models
- Self-evolving prompts

## Important Considerations

1. **Korean Market Focus**: All user-facing content should support Korean language
2. **Cost Optimization**: Implement caching aggressively to reduce LLM API costs
3. **Database Migrations**: Always use Flyway for schema changes
4. **Testing Requirements**: Maintain 80%+ code coverage
5. **API Documentation**: Update OpenAPI specs when adding/modifying endpoints

## Current Implementation Status

### Completed Tasks
- ✅ TASK-001: Spring Boot 3.x + Kotlin project setup with Docker Compose
- ✅ TASK-002: PostgreSQL database configuration with Flyway migrations
- ✅ TASK-003: Core infrastructure classes (BaseEntity, ApiResponse, Exception Handling)
- ✅ TASK-004: Complete domain model implementation with JPA entities
- ✅ TASK-005: Profile CRUD API with experiences, projects, and technical skills management
- ✅ TASK-006: Google Gemini API integration with LLMProvider interface
- ✅ TASK-007: Two-stage interview question generation service implementation

### Current Development Notes
- Application runs on port 8090 due to port conflicts
- PostgreSQL runs on port 5434 instead of standard 5432
- Fixed Hibernate MultipleBagFetchException by separating entity fetching queries
- All API endpoints tested and working correctly
- Comprehensive error handling and validation implemented
- LLM integration with Gemini API functional (requires GEMINI_API_KEY environment variable)
- Prompt template management system implemented
- Caching enabled for LLM responses

### LLM Integration Details
- **Provider Interface**: Extensible design supporting multiple LLM providers
- **Current Provider**: Google Gemini (gemini-1.5-flash model)
- **Features**: Text generation, structured JSON responses, chat conversations
- **Templates**: Pre-configured prompts for interview questions, learning paths, resume analysis
- **Test Endpoints**: Available at `/api/v1/llm/test/*` for development

### Ready for Next Phase
- TASK-008: Learning path generation service
- TASK-009: Multi-Agent system for advanced question generation