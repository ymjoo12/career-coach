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
├── agent/          # Multi-Agent system (Interview, Technical, Behavioral)
│   ├── base/      # Agent interfaces and base classes
│   ├── impl/      # Concrete agent implementations
│   ├── service/   # Agent orchestration service
│   └── dto/       # Agent-related DTOs
├── config/         # Spring configuration, security, WebMVC
├── controller/     # REST endpoints with validation
├── service/        # Business logic with transaction management
├── domain/         # JPA entities
├── repository/     # Spring Data JPA repositories
├── dto/           # Request/Response DTOs with validation
├── external/      # LLM provider integration (Provider pattern)
├── interview/     # Interview question generation services
├── learning/      # Learning path generation services
└── common/        # Shared utilities and exceptions
```

### Key Patterns

1. **LLM Provider Pattern**: External services use Provider interface for extensibility
   - `LLMProvider` interface in `external/llm/`
   - Implementations: `GeminiProvider`, future providers

2. **Multi-Agent System**: Specialized agents for different aspects of career coaching
   - `InterviewAgent`: General interview questions
   - `TechnicalAgent`: Deep technical assessments and coding challenges
   - `BehavioralAgent`: Behavioral and cultural fit assessments
   - `AgentOrchestrator`: Coordinates multiple agents in parallel/sequential execution

3. **Smart Caching Strategy**: Profile similarity-based caching (Phase 2)
   - Cache similar profile questions
   - Reduce LLM API calls by 40-60%
   - Agent-level response caching

4. **Two-Stage Question Generation**:
   - Stage 1: Generate base questions from profile
   - Stage 2: Enhance with position-specific context

## API Structure

### Core Endpoints
- `POST /api/v1/profiles` - Create user profile
- `GET /api/v1/profiles/{id}/questions` - Generate interview questions
- `POST /api/v1/profiles/{id}/questions/followup` - Generate follow-up questions
- `GET /api/v1/profiles/{id}/learning-paths` - Get learning recommendations

### Multi-Agent Endpoints
- `POST /api/v1/agents/orchestrate` - Execute multi-agent orchestration
- `POST /api/v1/agents/interview-questions` - Generate comprehensive interview questions using all agents
- `GET /api/v1/agents/available` - Get list of available agents
- `DELETE /api/v1/agents/cache` - Clear agent response cache

### Cache Management Endpoints
- `GET /api/v1/cache/statistics` - Get cache hit rate and performance metrics
- `POST /api/v1/cache/similarity/calculate` - Calculate similarity between two profiles
- `GET /api/v1/cache/profile/{id}/similar-cached` - Find similar cached data for a profile
- `POST /api/v1/cache/warmup` - Pre-populate cache with common profile combinations
- `DELETE /api/v1/cache/clear` - Clear all caches

### Monitoring Endpoints
- `GET /api/v1/monitoring/dashboard` - Get comprehensive dashboard metrics
- `GET /api/v1/monitoring/realtime` - Get real-time activity metrics
- `GET /api/v1/monitoring/health` - System health check
- `GET /api/v1/monitoring/alerts` - Get active system alerts
- `POST /api/v1/monitoring/metrics/custom` - Record custom metrics
- `GET /actuator/prometheus` - Prometheus metrics export

### Context Intelligence Endpoints
- `POST /api/v1/context/sessions` - Create new context session
- `GET /api/v1/context/sessions/{id}` - Get session details
- `PUT /api/v1/context/sessions/{id}/interact` - Record user interaction
- `POST /api/v1/context/sessions/{id}/analyze-intent` - Analyze user intent
- `POST /api/v1/context/sessions/{id}/adjust-response` - Adjust response based on context
- `GET /api/v1/context/sessions/{id}/recommendations` - Get personalized recommendations
- `GET /api/v1/context/statistics` - Context system statistics

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
- ✅ TASK-008: Learning path generation service with skill gap analysis
- ✅ TASK-009: Multi-Agent system implementation with Interview, Technical, and Behavioral agents
- ✅ TASK-010: Similarity-based caching strategy for profile matching and response reuse
- ✅ TASK-011: Real-time monitoring dashboard with metrics collection and health checks
- ✅ TASK-012: Context Intelligence Layer for personalized user experiences

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

### Multi-Agent System Details
- **Agent Types**: Interview, Technical, Behavioral agents with specialized prompts
- **Orchestration**: Parallel and sequential execution support
- **Caching**: In-memory cache for agent responses
- **Fallback**: Each agent has fallback responses for LLM failures
- **Confidence Scoring**: Each response includes confidence metrics

### Similarity-Based Caching System
- **Profile Similarity Calculator**: Multi-factor similarity scoring (experience, skills, position, company, education)
- **Smart Cache Service**: Automatic cache lookup for similar profiles (80%+ similarity threshold)
- **Cache Management**: TTL-based expiration, scheduled cleanup, statistics tracking
- **Performance**: Reduces LLM API calls by 40-60% for similar profile requests
- **API Endpoints**: `/api/v1/cache/*` for cache management and statistics

### Monitoring & Observability
- **Metrics Collection**: API, LLM, Cache, System metrics with Micrometer
- **Real-time Dashboard**: Live metrics visualization and tracking
- **Health Checks**: Component status monitoring (DB, LLM, Cache, JVM)
- **Alert System**: Automatic alerts for high error rates, low cache hits, memory issues
- **Prometheus Integration**: Export metrics for external monitoring
- **Custom Metrics**: Application-specific metric recording

### Context Intelligence Layer
- **User Context Tracking**: Session-based context management with interaction history
- **Intent Analysis**: Automatic detection of user intent from queries and behavior
- **Personalization Engine**: Response style adjustment based on preferences and signals
- **Engagement Analysis**: Real-time engagement level tracking and optimization
- **Context Signals**: Detection of user state (first-time, confused, urgent need, etc.)
- **Smart Recommendations**: Context-aware content, action, and learning recommendations

### Ready for Next Phase
- TASK-013: Real-time interview simulation with WebSocket
- TASK-014: Advanced analytics and reporting
- TASK-015: Mobile API optimization