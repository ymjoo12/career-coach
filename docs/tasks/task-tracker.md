# Task Tracker - Career Coach API

## 완료된 작업

### ✅ TASK-001: 프로젝트 초기 설정
**완료일**: 2025-08-20
- Spring Boot 3.2.2 + Kotlin 1.9.22 프로젝트 생성
- Gradle 8.5 설정
- 기본 의존성 추가 (JPA, PostgreSQL, Flyway, OpenAPI)
- application.yml 설정 (dev/test 프로파일 포함)

### ✅ TASK-002: Docker Compose 설정
**완료일**: 2025-08-20
- PostgreSQL 16 Alpine 컨테이너 구성
- 개발용 DB (포트 5434) 및 테스트용 DB (포트 5433) 설정
- Flyway 마이그레이션으로 초기 스키마 생성
- 모든 테이블 및 인덱스 생성 완료

**생성된 테이블**:
- profiles (프로필 정보)
- experiences (경력 정보)
- projects (프로젝트 정보)
- technical_skills (기술 스킬)
- interview_question_sets (면접 질문 세트)
- interview_questions (개별 질문)
- learning_roadmaps (학습 로드맵)
- learning_roadmap_items (학습 로드맵 아이템)

## 진행 예정 작업

### 🔄 TASK-003: 기본 패키지 구조 생성 및 설정 클래스
- config 패키지: WebConfig, SwaggerConfig
- common 패키지: BaseEntity, ApiResponse
- exception 패키지: 커스텀 예외 클래스

### 📋 TASK-004: Profile 도메인 모델 및 JPA 엔티티
- Profile, Experience, Project, TechnicalSkill 엔티티
- JPA Auditing 설정
- 연관관계 매핑

### 📋 TASK-005: Profile CRUD API
- ProfileController
- ProfileService
- ProfileRepository
- ProfileDTO (Request/Response)

### 📋 TASK-006: Google Gemini API 통합
- LLMProvider 인터페이스
- GeminiProvider 구현체
- LLMConfig 설정

### 📋 TASK-007: 2단계 질문 생성 서비스
- QuestionGenerationService
- 프롬프트 엔지니어링
- 캐싱 로직

### 📋 TASK-008: 학습 경로 생성 서비스
- LearningPathService
- 스킬 갭 분석
- 리소스 추천

## 현재 환경 정보

### 서버 정보
- **애플리케이션 포트**: 8090
- **PostgreSQL 포트**: 5434 (개발), 5433 (테스트)
- **Swagger UI**: http://localhost:8090/swagger-ui
- **Actuator**: http://localhost:8090/actuator

### 실행 명령어
```bash
# Docker 컨테이너 시작
docker compose up -d postgres

# 애플리케이션 실행
./gradlew bootRun

# 테스트 실행
./gradlew test

# 빌드
./gradlew clean build
```

## 주의사항
- GEMINI_API_KEY 환경변수 설정 필요 (TASK-006에서)
- 포트 충돌 주의 (8090, 5434 사용 중)
- Kotlin 코루틴 사용 시 비동기 처리 고려