# 커리어 코치 챗봇 API 구현 가이드

## 목차
1. [프로젝트 설정 및 초기화](#1-프로젝트-설정-및-초기화)
2. [개발 워크플로우](#2-개발-워크플로우)
3. [아키텍처 및 패키지 구조](#3-아키텍처-및-패키지-구조)
4. [데이터베이스 설계](#4-데이터베이스-설계)
5. [Phase 1 구현 가이드](#5-phase-1-구현-가이드)
6. [테스트 전략](#6-테스트-전략)
7. [배포 및 운영](#7-배포-및-운영)

## 1. 프로젝트 설정 및 초기화

### 1.1 프로젝트 구조 생성

```bash
# 프로젝트 생성
mkdir career-coach-api
cd career-coach-api

# Git 초기화
git init
git remote add origin <repository-url>

# 브랜치 전략 설정
git checkout -b develop
git push -u origin develop
```

### 1.2 기본 프로젝트 구조
```
career-coach-api/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/careercoach/
│   │   │       ├── CareerCoachApplication.kt
│   │   │       ├── config/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── domain/
│   │   │       ├── repository/
│   │   │       ├── dto/
│   │   │       └── external/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   └── test/
│       └── kotlin/
├── docker/
│   ├── docker-compose.yml
│   └── Dockerfile
├── docs/
│   ├── api/
│   ├── tasks/
│   └── README.md
├── build.gradle.kts
└── README.md
```

### 1.3 build.gradle.kts 설정
```kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.1.2"
    id("io.spring.dependency-management") version "1.1.2"
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.spring") version "1.9.0"
    kotlin("plugin.jpa") version "1.9.0"
}

group = "com.careercoach"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    
    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    
    // HTTP Client (for LLM API)
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")
    
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

## 2. 개발 워크플로우

### 2.1 Task 관리 구조

**docs/tasks/task-tracker.md** 파일 생성:
```markdown
# Task Tracker

## Phase 1: MVP Development

### 🏗️ Infrastructure Setup
- [ ] TASK-001: 프로젝트 초기 설정
- [ ] TASK-002: 데이터베이스 설정 (Docker + PostgreSQL)
- [ ] TASK-003: 기본 Spring Boot 설정

### 💾 Database Layer
- [ ] TASK-004: 데이터베이스 스키마 설계
- [ ] TASK-005: JPA Entity 클래스 구현
- [ ] TASK-006: Repository 인터페이스 구현

### 🔌 External Integration
- [ ] TASK-007: LLM API 연동 기반 구조
- [ ] TASK-008: Gemini API 클라이언트 구현

### 🎯 Core Business Logic
- [ ] TASK-009: 프로필 관리 서비스
- [ ] TASK-010: 질문 생성 서비스
- [ ] TASK-011: 학습 경로 생성 서비스

### 🌐 API Layer
- [ ] TASK-012: 프로필 REST API
- [ ] TASK-013: 면접 질문 REST API
- [ ] TASK-014: 학습 경로 REST API

### ✅ Testing & Documentation
- [ ] TASK-015: 단위 테스트 작성
- [ ] TASK-016: 통합 테스트 작성
- [ ] TASK-017: API 문서화

## Completed Tasks
(여기에 완료된 task들을 날짜와 함께 이동)

## In Progress
- **Current:** 
- **Assignee:** 
- **Start Date:** 
- **Expected Completion:** 

## Notes
- 각 task는 별도 브랜치에서 작업
- 통합 테스트 완료 후 develop 브랜치로 머지
- E2E 테스트 완료 후 main 브랜치로 머지
```

### 2.2 브랜치 전략

```bash
# 새 task 시작
git checkout develop
git pull origin develop
git checkout -b feature/TASK-XXX-description

# 작업 완료 후
git add .
git commit -m "feat: TASK-XXX - 작업 내용 요약"
git push origin feature/TASK-XXX-description

# PR 생성 후 리뷰 및 머지
# 머지 완료 후 브랜치 삭제
git checkout develop
git pull origin develop
git branch -d feature/TASK-XXX-description
git push origin --delete feature/TASK-XXX-description
```

### 2.3 커밋 메시지 컨벤션
```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 포맷팅, 세미콜론 누락 등
refactor: 코드 리팩토링
test: 테스트 코드 추가/수정
chore: 빌드 업무 수정, 패키지 매니저 설정 등

예시:
feat: TASK-009 - 프로필 생성 API 구현
test: TASK-015 - 프로필 서비스 단위 테스트 추가
docs: API 문서 업데이트
```

## 3. 아키텍처 및 패키지 구조

### 3.1 레이어드 아키텍처
```
┌─────────────────┐
│   Controller    │  ← REST API 엔드포인트
├─────────────────┤
│     Service     │  ← 비즈니스 로직
├─────────────────┤
│   Repository    │  ← 데이터 접근
├─────────────────┤
│    External     │  ← 외부 API 연동
└─────────────────┘
```

### 3.2 패키지 구조 상세
```kotlin
com.careercoach
├── config/                    // 설정 클래스들
│   ├── DatabaseConfig.kt
│   ├── LlmConfig.kt
│   ├── WebConfig.kt
│   └── OpenApiConfig.kt
├── controller/                // REST 컨트롤러
│   ├── ProfileController.kt
│   ├── InterviewController.kt
│   └── LearningController.kt
├── service/                   // 비즈니스 로직
│   ├── ProfileService.kt
│   ├── QuestionGenerationService.kt
│   ├── LearningPathService.kt
│   └── cache/
│       └── CacheService.kt
├── domain/                    // 도메인 엔티티
│   ├── Profile.kt
│   ├── Experience.kt
│   ├── Skill.kt
│   ├── InterviewQuestion.kt
│   └── LearningRoadmap.kt
├── repository/                // 데이터 리포지토리
│   ├── ProfileRepository.kt
│   ├── InterviewQuestionRepository.kt
│   └── LearningRoadmapRepository.kt
├── dto/                       // 데이터 전송 객체
│   ├── request/
│   │   ├── ProfileCreateRequest.kt
│   │   └── QuestionGenerationRequest.kt
│   └── response/
│       ├── ProfileResponse.kt
│       └── QuestionSetResponse.kt
├── external/                  // 외부 API 연동
│   ├── llm/
│   │   ├── LlmProvider.kt
│   │   ├── GeminiProvider.kt
│   │   └── dto/
│   └── data/
│       └── MarketDataProvider.kt
├── common/                    // 공통 유틸리티
│   ├── exception/
│   ├── validation/
│   └── util/
└── CareerCoachApplication.kt  // 메인 애플리케이션 클래스
```

## 4. 데이터베이스 설계

### 4.1 Docker 환경 설정

**docker/docker-compose.yml**
```yaml
version: '3.8'
services:
  postgresql:
    image: postgres:15
    container_name: career-coach-db
    environment:
      POSTGRES_DB: career_coach
      POSTGRES_USER: career_user
      POSTGRES_PASSWORD: career_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - career-coach-network

  redis:
    image: redis:7-alpine
    container_name: career-coach-redis
    ports:
      - "6379:6379"
    networks:
      - career-coach-network

volumes:
  postgres_data:

networks:
  career-coach-network:
    driver: bridge
```

### 4.2 데이터베이스 스키마 (Flyway Migration)

**src/main/resources/db/migration/V1__Initial_schema.sql**
```sql
-- 프로필 기본 정보
CREATE TABLE profiles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    total_experience VARCHAR(50),
    current_role VARCHAR(200),
    target_role VARCHAR(200),
    target_industry VARCHAR(100),
    location VARCHAR(100),
    work_type VARCHAR(20) DEFAULT 'HYBRID',
    profile_completeness DECIMAL(3,2) DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 경력 정보
CREATE TABLE experiences (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT REFERENCES profiles(id) ON DELETE CASCADE,
    company VARCHAR(200) NOT NULL,
    company_size VARCHAR(20),
    position VARCHAR(200) NOT NULL,
    level_title VARCHAR(50),
    start_date DATE,
    end_date DATE,
    total_months INTEGER,
    department VARCHAR(100),
    team_size INTEGER,
    role_type VARCHAR(20) DEFAULT '팀원',
    reason_for_leaving TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 프로젝트 정보
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    experience_id BIGINT REFERENCES experiences(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    duration VARCHAR(50),
    team_size INTEGER,
    my_role VARCHAR(100),
    business_impact TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 프로젝트 기술 스택
CREATE TABLE project_technologies (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
    technology VARCHAR(100) NOT NULL
);

-- 프로젝트 성과
CREATE TABLE project_achievements (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
    achievement TEXT NOT NULL
);

-- 프로젝트 도전과제
CREATE TABLE project_challenges (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
    challenge TEXT NOT NULL
);

-- 기술 스킬
CREATE TABLE technical_skills (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT REFERENCES profiles(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL, -- PROGRAMMING_LANGUAGE, FRAMEWORK, DATABASE, etc.
    name VARCHAR(100) NOT NULL,
    proficiency VARCHAR(20) NOT NULL, -- BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    experience_months INTEGER,
    last_used DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 기술 스킬 상세 정보 (버전, 관련 기술 등)
CREATE TABLE skill_details (
    id BIGSERIAL PRIMARY KEY,
    technical_skill_id BIGINT REFERENCES technical_skills(id) ON DELETE CASCADE,
    detail_type VARCHAR(50), -- VERSION, RELATED_SKILL, CERTIFICATION, PROJECT
    detail_value VARCHAR(200)
);

-- 소프트 스킬
CREATE TABLE soft_skills (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT REFERENCES profiles(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    level_value VARCHAR(20), -- BEGINNER, INTERMEDIATE, ADVANCED
    examples TEXT[]
);

-- 커리어 목표
CREATE TABLE career_goals (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT REFERENCES profiles(id) ON DELETE CASCADE,
    term_type VARCHAR(20) NOT NULL, -- SHORT_TERM, LONG_TERM
    position VARCHAR(200),
    timeline VARCHAR(50),
    target_salary_min INTEGER,
    target_salary_max INTEGER,
    currency VARCHAR(10) DEFAULT 'KRW_10000'
);

-- 목표 회사들
CREATE TABLE target_companies (
    id BIGSERIAL PRIMARY KEY,
    career_goal_id BIGINT REFERENCES career_goals(id) ON DELETE CASCADE,
    company_name VARCHAR(200) NOT NULL
);

-- 동기 및 선호사항
CREATE TABLE motivations (
    id BIGSERIAL PRIMARY KEY,
    career_goal_id BIGINT REFERENCES career_goals(id) ON DELETE CASCADE,
    motivation TEXT NOT NULL
);

-- 면접 질문 세트
CREATE TABLE interview_question_sets (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT REFERENCES profiles(id) ON DELETE CASCADE,
    generation_type VARCHAR(20) DEFAULT 'BASIC', -- BASIC, CACHED_ADAPTED, MULTI_AGENT
    target_company VARCHAR(200),
    question_count INTEGER DEFAULT 5,
    difficulty VARCHAR(20),
    personalization_level DECIMAL(3,2),
    generation_cost DECIMAL(8,4),
    cache_utilized BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 개별 면접 질문
CREATE TABLE interview_questions (
    id BIGSERIAL PRIMARY KEY,
    question_set_id BIGINT REFERENCES interview_question_sets(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    question TEXT NOT NULL,
    intent TEXT,
    sequence_order INTEGER,
    experience_match DECIMAL(3,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 질문 평가 포인트
CREATE TABLE question_evaluation_points (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES interview_questions(id) ON DELETE CASCADE,
    evaluation_point VARCHAR(200) NOT NULL
);

-- 좋은 답변 요소
CREATE TABLE good_answer_elements (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES interview_questions(id) ON DELETE CASCADE,
    element TEXT NOT NULL
);

-- 흔한 실수
CREATE TABLE common_mistakes (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES interview_questions(id) ON DELETE CASCADE,
    mistake TEXT NOT NULL
);

-- 후속 질문
CREATE TABLE follow_up_questions (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT REFERENCES interview_questions(id) ON DELETE CASCADE,
    follow_up_question TEXT NOT NULL
);

-- 학습 로드맵
CREATE TABLE learning_roadmaps (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT REFERENCES profiles(id) ON DELETE CASCADE,
    current_level VARCHAR(200),
    target_level VARCHAR(200),
    market_fit DECIMAL(3,2),
    time_to_goal VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 스킬 갭 분석
CREATE TABLE skill_gaps (
    id BIGSERIAL PRIMARY KEY,
    roadmap_id BIGINT REFERENCES learning_roadmaps(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    importance VARCHAR(20) NOT NULL, -- HIGH, MEDIUM, LOW
    current_level INTEGER,
    target_level INTEGER,
    market_demand VARCHAR(20) NOT NULL -- VERY_HIGH, HIGH, MEDIUM, LOW
);

-- 학습 단계
CREATE TABLE learning_phases (
    id BIGSERIAL PRIMARY KEY,
    roadmap_id BIGINT REFERENCES learning_roadmaps(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    duration VARCHAR(50),
    goal TEXT,
    priority VARCHAR(20), -- HIGH, MEDIUM, LOW
    phase_order INTEGER
);

-- 학습 태스크
CREATE TABLE learning_tasks (
    id BIGSERIAL PRIMARY KEY,
    phase_id BIGINT REFERENCES learning_phases(id) ON DELETE CASCADE,
    task_name VARCHAR(200) NOT NULL,
    task_type VARCHAR(50), -- LEARNING, PROJECT, PRACTICE
    estimated_hours INTEGER,
    deadline VARCHAR(50),
    success_criteria TEXT,
    task_order INTEGER
);

-- 학습 리소스
CREATE TABLE learning_resources (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT REFERENCES learning_tasks(id) ON DELETE CASCADE,
    resource_type VARCHAR(50), -- BOOK, COURSE, VIDEO, ARTICLE
    title VARCHAR(200) NOT NULL,
    provider VARCHAR(100),
    cost INTEGER,
    rating DECIMAL(2,1),
    duration VARCHAR(50),
    url VARCHAR(500)
);

-- 학습 결과물
CREATE TABLE task_deliverables (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT REFERENCES learning_tasks(id) ON DELETE CASCADE,
    deliverable TEXT NOT NULL
);

-- 피드백 (Phase 2에서 사용)
CREATE TABLE feedback (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL, -- QUESTION_SET, ROADMAP
    entity_id BIGINT NOT NULL,
    feedback_type VARCHAR(50), -- QUALITY, USEFULNESS, ACCURACY
    rating INTEGER CHECK (rating >= 1 AND rating <= 10),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX idx_profiles_target_role ON profiles(target_role);
CREATE INDEX idx_experiences_profile_id ON experiences(profile_id);
CREATE INDEX idx_technical_skills_profile_id ON technical_skills(profile_id);
CREATE INDEX idx_interview_questions_set_id ON interview_questions(question_set_id);
CREATE INDEX idx_learning_phases_roadmap_id ON learning_phases(roadmap_id);
CREATE INDEX idx_feedback_entity ON feedback(entity_type, entity_id);

-- 트리거: updated_at 자동 갱신
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_profiles_updated_at 
    BEFORE UPDATE ON profiles 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 4.3 JPA Entity 클래스 구현 가이드

**중요 유의사항:**
1. **연관관계 매핑 시 지연 로딩(LAZY) 사용**
2. **Cascade 설정 신중하게 결정**
3. **@JsonIgnore로 무한 참조 방지**
4. **Audit 필드 공통화**

**domain/Profile.kt**
```kotlin
@Entity
@Table(name = "profiles")
data class Profile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, length = 100)
    val name: String,
    
    @Column(name = "total_experience", length = 50)
    val totalExperience: String?,
    
    @Column(name = "current_role", length = 200)
    val currentRole: String?,
    
    @Column(name = "target_role", length = 200)
    val targetRole: String?,
    
    @Column(name = "target_industry", length = 100)
    val targetIndustry: String?,
    
    @Column(length = 100)
    val location: String?,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "work_type")
    val workType: WorkType = WorkType.HYBRID,
    
    @Column(name = "profile_completeness", precision = 3, scale = 2)
    val profileCompleteness: Double = 0.0,
    
    @OneToMany(mappedBy = "profile", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val experiences: List<Experience> = emptyList(),
    
    @OneToMany(mappedBy = "profile", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val technicalSkills: List<TechnicalSkill> = emptyList(),
    
    @OneToMany(mappedBy = "profile", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val softSkills: List<SoftSkill> = emptyList(),
    
    @OneToMany(mappedBy = "profile", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnore
    val careerGoals: List<CareerGoal> = emptyList()
    
) : BaseTimeEntity()

enum class WorkType {
    OFFICE, HYBRID, REMOTE
}
```

**common/BaseTimeEntity.kt**
```kotlin
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeEntity {
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: LocalDateTime
        protected set
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: LocalDateTime
        protected set
}
```

## 5. Phase 1 구현 가이드

### 5.1 TASK-001: 프로젝트 초기 설정

**브랜치:** `feature/TASK-001-project-setup`

**체크리스트:**
- [ ] Spring Boot 프로젝트 생성
- [ ] build.gradle.kts 의존성 설정
- [ ] application.yml 기본 설정
- [ ] Docker Compose 설정
- [ ] README.md 작성

**구현 단계:**
1. Spring Initializr 또는 IDE에서 프로젝트 생성
2. 필요한 의존성 추가 (위의 build.gradle.kts 참조)
3. 기본 설정 파일들 생성
4. Docker 환경 설정
5. 프로젝트 실행 확인

**테스트:**
```bash
# 프로젝트 빌드 테스트
./gradlew build

# 애플리케이션 실행 테스트
./gradlew bootRun

# Docker 환경 테스트
docker-compose -f docker/docker-compose.yml up -d
```

**완료 조건:**
- 애플리케이션이 정상적으로 실행됨
- 데이터베이스 연결 성공
- Swagger UI 접근 가능 (http://localhost:8080/swagger-ui.html)

---

### 5.2 TASK-002: 데이터베이스 설정

**브랜치:** `feature/TASK-002-database-setup`

**체크리스트:**
- [ ] Flyway 설정
- [ ] 초기 스키마 마이그레이션 스크립트 작성
- [ ] JPA 설정 및 검증
- [ ] 데이터베이스 연결 테스트

**구현 단계:**

1. **application.yml 데이터베이스 설정**
```yaml
spring:
  application:
    name: career-coach-api
  
  datasource:
    url: jdbc:postgresql://localhost:5432/career_coach
    username: career_user
    password: career_password
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

2. **Flyway 마이그레이션 스크립트 작성** (위의 V1__Initial_schema.sql 참조)

3. **JPA Auditing 설정**
```kotlin
@Configuration
@EnableJpaAuditing
class JpaConfig
```

**테스트:**
```kotlin
@DataJpaTest
@TestPropertySource(properties = [
    "spring.jpa.hibernate.ddl-auto=create-drop"
])
class DatabaseConnectionTest {
    
    @Autowired
    private lateinit var testEntityManager: TestEntityManager
    
    @Test
    fun `데이터베이스 연결 테스트`() {
        // 기본 연결 확인
        assertThat(testEntityManager).isNotNull
    }
}
```

**완료 조건:**
- Flyway 마이그레이션 성공
- 모든 테이블이 정상적으로 생성됨
- JPA Auditing 동작 확인

---

### 5.3 TASK-004: JPA Entity 클래스 구현

**브랜치:** `feature/TASK-004-entity-implementation`

**주요 유의사항:**
1. **연관관계 매핑 주의사항:**
   - 양방향 연관관계 시 무한 참조 방지 (@JsonIgnore)
   - 지연 로딩(LAZY) 기본 사용
   - Cascade 타입 신중하게 선택

2. **성능 고려사항:**
   - N+1 문제 방지를 위한 fetch join 전략 수립
   - 불필요한 연관관계 로딩 방지

**구현 순서:**
1. BaseTimeEntity (공통 Audit 필드)
2. Profile (기본 엔티티)
3. Experience, Project (1:N 관계)
4. TechnicalSkill, SoftSkill (1:N 관계)
5. CareerGoal (1:N 관계)

**예시 구현:**
```kotlin
@Entity
@Table(name = "experiences")
data class Experience(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,
    
    @Column(nullable = false, length = 200)
    val company: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "company_size")
    val companySize: CompanySize?,
    
    // ... 기타 필드들
    
    @OneToMany(mappedBy = "experience", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val projects: List<Project> = emptyList()
    
) : BaseTimeEntity()
```

**테스트:**
```kotlin
@DataJpaTest
class EntityRelationshipTest {
    
    @Autowired
    private lateinit var testEntityManager: TestEntityManager
    
    @Test
    fun `프로필과 경력 연관관계 테스트`() {
        // given
        val profile = Profile(name = "테스트", currentRole = "개발자")
        val savedProfile = testEntityManager.persistAndFlush(profile)
        
        val experience = Experience(
            profile = savedProfile,
            company = "테스트회사",
            companySize = CompanySize.MEDIUM
        )
        
        // when
        val savedExperience = testEntityManager.persistAndFlush(experience)
        
        // then
        assertThat(savedExperience.profile.id).isEqualTo(savedProfile.id)
    }
}
```

---

### 5.4 TASK-005: Repository 인터페이스 구현

**브랜치:** `feature/TASK-005-repository-implementation`

**구현 가이드:**

```kotlin
@Repository
interface ProfileRepository : JpaRepository<Profile, Long> {
    
    fun findByTargetRoleContainingIgnoreCase(targetRole: String): List<Profile>
    
    fun findByTargetIndustryAndLocation(targetIndustry: String, location: String): List<Profile>
    
    @Query("""
        SELECT p FROM Profile p 
        LEFT JOIN FETCH p.experiences e 
        LEFT JOIN FETCH e.projects 
        WHERE p.id = :profileId
    """)
    fun findByIdWithExperiences(profileId: Long): Profile?
    
    @Query("""
        SELECT p FROM Profile p 
        LEFT JOIN FETCH p.technicalSkills ts 
        WHERE p.id = :profileId
    """)
    fun findByIdWithTechnicalSkills(profileId: Long): Profile?
}
```

**Repository 테스트:**
```kotlin
@DataJpaTest
class ProfileRepositoryTest {
    
    @Autowired
    private lateinit var profileRepository: ProfileRepository
    
    @Autowired
    private lateinit var testEntityManager: TestEntityManager
    
    @Test
    fun `타겟 역할로 프로필 검색 테스트`() {
        // given
        val profile1 = Profile(name = "개발자1", targetRole = "백엔드 개발자")
        val profile2 = Profile(name = "개발자2", targetRole = "프론트엔드 개발자")
        testEntityManager.persistAndFlush(profile1)
        testEntityManager.persistAndFlush(profile2)
        
        // when
        val result = profileRepository.findByTargetRoleContainingIgnoreCase("백엔드")
        
        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("개발자1")
    }
}
```

---

### 5.5 TASK-007: LLM API 연동 기반 구조

**브랜치:** `feature/TASK-007-llm-integration`

**구현 구조:**
```kotlin
// 인터페이스 정의
interface LlmProvider {
    suspend fun generateResponse(prompt: String, config: LlmConfig): LlmResponse
    fun getProviderName(): String
    fun getMaxTokens(): Int
    fun getCostPerToken(): Double
}

// 설정 클래스
data class LlmConfig(
    val maxTokens: Int = 4096,
    val temperature: Double = 0.7,
    val topP: Double = 0.9
)

data class LlmResponse(
    val content: String,
    val tokensUsed: Int,
    val cost: Double,
    val provider: String
)

// Gemini 구현체
@Component
class GeminiProvider(
    @Value("\${llm.gemini.api-key}") private val apiKey: String,
    private val webClient: WebClient
) : LlmProvider {
    
    companion object {
        private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
    }
    
    override suspend fun generateResponse(prompt: String, config: LlmConfig): LlmResponse {
        return try {
            val request = buildGeminiRequest(prompt, config)
            val response = webClient.post()
                .uri(GEMINI_API_URL)
                .header("Authorization", "Bearer $apiKey")
                .bodyValue(request)
                .retrieve()
                .awaitBody<GeminiApiResponse>()
            
            parseGeminiResponse(response)
        } catch (e: WebClientResponseException) {
            throw LlmApiException("Gemini API 호출 실패: ${e.message}", e)
        }
    }
    
    private fun buildGeminiRequest(prompt: String, config: LlmConfig): GeminiRequest {
        return GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                maxOutputTokens = config.maxTokens,
                temperature = config.temperature,
                topP = config.topP
            )
        )
    }
}

// 팩토리 패턴
@Component
class LlmProviderFactory {
    
    fun createProvider(type: LlmProviderType): LlmProvider {
        return when(type) {
            LlmProviderType.GEMINI -> geminiProvider
            // Phase 2에서 추가: OPENAI, ANTHROPIC
        }
    }
}
```

**설정 파일 (application.yml):**
```yaml
llm:
  provider: GEMINI
  gemini:
    api-key: ${GEMINI_API_KEY:your-api-key-here}
    model: gemini-pro
    max-tokens: 4096
```

**테스트:**
```kotlin
@SpringBootTest
class GeminiProviderTest {
    
    @Autowired
    private lateinit var geminiProvider: GeminiProvider
    
    @Test
    fun `Gemini API 연동 테스트`() = runTest {
        // given
        val prompt = "안녕하세요. 테스트 메시지입니다."
        val config = LlmConfig(maxTokens = 100)
        
        // when
        val response = geminiProvider.generateResponse(prompt, config)
        
        // then
        assertThat(response.content).isNotBlank()
        assertThat(response.provider).isEqualTo("GEMINI")
        assertThat(response.tokensUsed).isGreaterThan(0)
    }
}
```

---

### 5.6 TASK-009: 프로필 관리 서비스

**브랜치:** `feature/TASK-009-profile-service`

**서비스 구현:**
```kotlin
@Service
@Transactional
class ProfileService(
    private val profileRepository: ProfileRepository,
    private val experienceRepository: ExperienceRepository,
    private val technicalSkillRepository: TechnicalSkillRepository
) {
    
    fun createProfile(request: ProfileCreateRequest): ProfileResponse {
        // 1. 기본 프로필 생성
        val profile = Profile(
            name = request.basicInfo.name,
            totalExperience = request.basicInfo.totalExperience,
            currentRole = request.basicInfo.currentRole,
            targetRole = request.basicInfo.targetRole,
            targetIndustry = request.basicInfo.targetIndustry,
            location = request.basicInfo.location,
            workType = request.basicInfo.workType ?: WorkType.HYBRID
        )
        
        val savedProfile = profileRepository.save(profile)
        
        // 2. 경력 정보 저장
        request.experiences?.let { experiences ->
            saveExperiences(savedProfile, experiences)
        }
        
        // 3. 기술 스킬 저장
        request.skills?.technical?.let { skills ->
            saveTechnicalSkills(savedProfile, skills)
        }
        
        // 4. 프로필 완성도 계산 및 업데이트
        val completeness = calculateProfileCompleteness(savedProfile)
        val updatedProfile = profileRepository.save(
            savedProfile.copy(profileCompleteness = completeness)
        )
        
        return ProfileResponse.from(updatedProfile)
    }
    
    @Transactional(readOnly = true)
    fun getProfile(profileId: Long): ProfileResponse {
        val profile = profileRepository.findByIdWithExperiences(profileId)
            ?: throw ProfileNotFoundException("프로필을 찾을 수 없습니다: $profileId")
        
        return ProfileResponse.from(profile)
    }
    
    fun updateProfile(profileId: Long, request: ProfileUpdateRequest): ProfileResponse {
        val existingProfile = profileRepository.findById(profileId)
            .orElseThrow { ProfileNotFoundException("프로필을 찾을 수 없습니다: $profileId") }
        
        // 업데이트 로직
        val updatedProfile = existingProfile.copy(
            name = request.basicInfo.name,
            totalExperience = request.basicInfo.totalExperience,
            // ... 기타 필드들
        )
        
        val savedProfile = profileRepository.save(updatedProfile)
        return ProfileResponse.from(savedProfile)
    }
    
    fun deleteProfile(profileId: Long) {
        if (!profileRepository.existsById(profileId)) {
            throw ProfileNotFoundException("프로필을 찾을 수 없습니다: $profileId")
        }
        
        profileRepository.deleteById(profileId)
    }
    
    private fun calculateProfileCompleteness(profile: Profile): Double {
        var score = 0.0
        val maxScore = 10.0
        
        // 기본 정보 완성도 (30%)
        if (profile.name.isNotBlank()) score += 1.0
        if (!profile.totalExperience.isNullOrBlank()) score += 1.0
        if (!profile.currentRole.isNullOrBlank()) score += 1.0
        
        // 경력 정보 완성도 (40%)
        if (profile.experiences.isNotEmpty()) score += 2.0
        if (profile.experiences.any { it.projects.isNotEmpty() }) score += 2.0
        
        // 기술 스킬 완성도 (30%)
        if (profile.technicalSkills.isNotEmpty()) score += 2.0
        if (profile.technicalSkills.size >= 5) score += 1.0
        
        return (score / maxScore).coerceIn(0.0, 1.0)
    }
}
```

**예외 처리:**
```kotlin
class ProfileNotFoundException(message: String) : RuntimeException(message)

class InvalidProfileDataException(message: String) : RuntimeException(message)

@ControllerAdvice
class GlobalExceptionHandler {
    
    @ExceptionHandler(ProfileNotFoundException::class)
    fun handleProfileNotFound(e: ProfileNotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse("PROFILE_NOT_FOUND", e.message ?: "프로필을 찾을 수 없습니다"))
    }
    
    @ExceptionHandler(InvalidProfileDataException::class)
    fun handleInvalidProfileData(e: InvalidProfileDataException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse("INVALID_PROFILE_DATA", e.message ?: "잘못된 프로필 데이터입니다"))
    }
}
```

**서비스 테스트:**
```kotlin
@ExtendWith(MockKExtension::class)
class ProfileServiceTest {
    
    @MockK
    private lateinit var profileRepository: ProfileRepository
    
    @MockK 
    private lateinit var experienceRepository: ExperienceRepository
    
    @InjectMockKs
    private lateinit var profileService: ProfileService
    
    @Test
    fun `프로필 생성 성공 테스트`() {
        // given
        val request = ProfileCreateRequest(
            basicInfo = BasicInfoRequest(
                name = "홍길동",
                totalExperience = "3년",
                currentRole = "백엔드 개발자"
            )
        )
        
        val savedProfile = Profile(
            id = 1L,
            name = "홍길동",
            totalExperience = "3년",
            currentRole = "백엔드 개발자"
        )
        
        every { profileRepository.save(any<Profile>()) } returns savedProfile
        
        // when
        val result = profileService.createProfile(request)
        
        // then
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.basicInfo.name).isEqualTo("홍길동")
        verify { profileRepository.save(any<Profile>()) }
    }
}
```

---

### 5.7 TASK-010: 질문 생성 서비스

**브랜치:** `feature/TASK-010-question-generation-service`

**핵심 구현 포인트:**
1. **프롬프트 템플릿 관리**
2. **LLM 응답 파싱 및 검증**
3. **캐싱 전략**
4. **에러 핸들링**

**서비스 구현:**
```kotlin
@Service
@Transactional
class QuestionGenerationService(
    private val profileService: ProfileService,
    private val llmProvider: LlmProvider,
    private val cacheService: CacheService,
    private val questionSetRepository: InterviewQuestionSetRepository
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(QuestionGenerationService::class.java)
    }
    
    suspend fun generateQuestions(request: QuestionGenerationRequest): QuestionSetResponse {
        logger.info("질문 생성 시작 - 프로필 ID: ${request.profileId}")
        
        // 1. 프로필 조회
        val profile = profileService.getProfile(request.profileId)
        
        // 2. 캐시 확인
        val cacheKey = generateCacheKey(profile, request)
        cacheService.getCached(cacheKey)?.let { cachedResult ->
            logger.info("캐시된 결과 반환 - 키: $cacheKey")
            return cachedResult
        }
        
        // 3. 프롬프트 생성
        val prompt = buildInterviewPrompt(profile, request)
        
        // 4. LLM 호출
        val llmResponse = try {
            llmProvider.generateResponse(prompt, LlmConfig(maxTokens = 3000))
        } catch (e: Exception) {
            logger.error("LLM API 호출 실패", e)
            throw QuestionGenerationException("질문 생성 중 오류가 발생했습니다", e)
        }
        
        // 5. 응답 파싱
        val questions = parseQuestionsFromResponse(llmResponse.content)
        
        // 6. 데이터베이스 저장
        val questionSet = saveQuestionSet(profile, request, questions, llmResponse)
        
        // 7. 캐싱
        val response = QuestionSetResponse.from(questionSet)
        cacheService.cache(cacheKey, response, Duration.ofHours(24))
        
        logger.info("질문 생성 완료 - 질문 수: ${questions.size}, 비용: ${llmResponse.cost}")
        return response
    }
    
    private fun buildInterviewPrompt(profile: ProfileResponse, request: QuestionGenerationRequest): String {
        val jobFunction = extractJobFunction(profile.basicInfo.targetRole ?: profile.basicInfo.currentRole)
        val template = getPromptTemplate(jobFunction)
        
        return template
            .replace("{TARGET_ROLE}", profile.basicInfo.targetRole ?: "지원 직무")
            .replace("{CURRENT_ROLE}", profile.basicInfo.currentRole ?: "현재 직무")
            .replace("{TOTAL_EXPERIENCE}", profile.basicInfo.totalExperience ?: "경력 미상")
            .replace("{TARGET_INDUSTRY}", profile.basicInfo.targetIndustry ?: "업계")
            .replace("{MAIN_SKILLS}", extractMainSkills(profile.skills?.technical))
            .replace("{KEY_PROJECTS}", extractKeyProjects(profile.experiences))
            .replace("{QUESTION_COUNT}", request.questionCount.toString())
            .replace("{DIFFICULTY}", request.difficulty?.toString() ?: "INTERMEDIATE")
    }
    
    private fun getPromptTemplate(jobFunction: JobFunction): String {
        return when(jobFunction) {
            JobFunction.DEVELOPER -> getGeneralPromptTemplate()
            JobFunction.DESIGNER -> getGeneralPromptTemplate()
            JobFunction.MARKETER -> getGeneralPromptTemplate()
            JobFunction.SALES -> getGeneralPromptTemplate()
            JobFunction.PLANNER -> getGeneralPromptTemplate()
            else -> getGeneralPromptTemplate()
        }
    }
    
    private fun getGeneralPromptTemplate(): String {
        return """
            당신은 {TARGET_INDUSTRY} 업계의 {TARGET_ROLE} 면접관입니다.
            다음 지원자의 이력서를 바탕으로 실제 면접에서 나올 법한 질문 {QUESTION_COUNT}개를 생성하세요.

            [지원자 정보]
            - 경력: {TOTAL_EXPERIENCE}
            - 현재 직무: {CURRENT_ROLE}
            - 목표 직무: {TARGET_ROLE}
            - 주요 기술/역량: {MAIN_SKILLS}
            - 핵심 프로젝트: {KEY_PROJECTS}

            [질문 생성 가이드라인]
            1. 기본 역량 확인 (30%) - 지원자의 기본적인 업무 능력과 지식
            2. 경험 기반 심화 질문 (40%) - 실제 경험한 프로젝트와 성과 중심
            3. 상황 대응 능력 (20%) - 가상의 상황에서의 문제해결 능력
            4. 성장 가능성 (10%) - 학습 의지와 발전 계획

            각 질문에 대해 다음 정보를 포함하여 JSON 형식으로 응답하세요:
            {
              "questions": [
                {
                  "category": "질문 카테고리 (BASIC_COMPETENCY, EXPERIENCE_BASED, SITUATION_HANDLING, GROWTH_POTENTIAL)",
                  "difficulty": "난이도 (BEGINNER, INTERMEDIATE, ADVANCED)",
                  "question": "면접 질문 내용",
                  "intent": "질문의 의도",
                  "evaluationPoints": ["평가 포인트1", "평가 포인트2"],
                  "goodAnswerElements": ["좋은 답변 요소1", "좋은 답변 요소2"],
                  "commonMistakes": ["흔한 실수1", "흔한 실수2"],
                  "followUpQuestions": ["후속 질문1", "후속 질문2"]
                }
              ]
            }

            반드시 유효한 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.
        """.trimIndent()
    }
    
    private fun parseQuestionsFromResponse(content: String): List<InterviewQuestionDto> {
        return try {
            val cleanContent = content
                .replace("```json", "")
                .replace("```", "")
                .trim()
                
            val jsonNode = objectMapper.readTree(cleanContent)
            val questionsNode = jsonNode.get("questions")
                ?: throw QuestionGenerationException("응답에서 questions 배열을 찾을 수 없습니다")
            
            questionsNode.map { questionNode ->
                InterviewQuestionDto(
                    category = QuestionCategory.valueOf(questionNode.get("category").asText()),
                    difficulty = QuestionDifficulty.valueOf(questionNode.get("difficulty").asText()),
                    question = questionNode.get("question").asText(),
                    intent = questionNode.get("intent").asText(),
                    evaluationPoints = questionNode.get("evaluationPoints").map { it.asText() },
                    goodAnswerElements = questionNode.get("goodAnswerElements").map { it.asText() },
                    commonMistakes = questionNode.get("commonMistakes").map { it.asText() },
                    followUpQuestions = questionNode.get("followUpQuestions").map { it.asText() }
                )
            }
        } catch (e: Exception) {
            logger.error("질문 파싱 실패: $content", e)
            throw QuestionGenerationException("LLM 응답 파싱에 실패했습니다", e)
        }
    }
    
    private fun generateCacheKey(profile: ProfileResponse, request: QuestionGenerationRequest): String {
        val keyComponents = listOf(
            profile.basicInfo.targetRole ?: "",
            profile.basicInfo.currentRole ?: "",
            profile.basicInfo.totalExperience ?: "",
            extractMainSkills(profile.skills?.technical),
            request.questionCount.toString(),
            request.difficulty?.toString() ?: "",
            request.targetCompany ?: ""
        )
        
        val keyString = keyComponents.joinToString("|")
        return "questions:" + keyString.hashCode().toString()
    }
}
```

**DTO 클래스들:**
```kotlin
data class QuestionGenerationRequest(
    val profileId: Long,
    val questionCount: Int = 5,
    val difficulty: QuestionDifficulty? = null,
    val targetCompany: String? = null,
    val focusAreas: List<String> = emptyList()
)

data class QuestionSetResponse(
    val id: Long,
    val profileId: Long,
    val questions: List<InterviewQuestionResponse>,
    val metadata: QuestionSetMetadata,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(questionSet: InterviewQuestionSet): QuestionSetResponse {
            return QuestionSetResponse(
                id = questionSet.id,
                profileId = questionSet.profileId,
                questions = questionSet.questions.map { InterviewQuestionResponse.from(it) },
                metadata = QuestionSetMetadata(
                    generationType = questionSet.generationType,
                    personalizationLevel = questionSet.personalizationLevel,
                    generationCost = questionSet.generationCost,
                    cacheUtilized = questionSet.cacheUtilized
                ),
                createdAt = questionSet.createdAt
            )
        }
    }
}

enum class QuestionCategory {
    BASIC_COMPETENCY, EXPERIENCE_BASED, SITUATION_HANDLING, GROWTH_POTENTIAL
}

enum class QuestionDifficulty {
    BEGINNER, INTERMEDIATE, ADVANCED
}
```

**서비스 테스트:**
```kotlin
@ExtendWith(MockKExtension::class)
class QuestionGenerationServiceTest {
    
    @MockK
    private lateinit var profileService: ProfileService
    
    @MockK
    private lateinit var llmProvider: LlmProvider
    
    @MockK
    private lateinit var cacheService: CacheService
    
    @MockK
    private lateinit var questionSetRepository: InterviewQuestionSetRepository
    
    @InjectMockKs
    private lateinit var questionGenerationService: QuestionGenerationService
    
    @Test
    fun `질문 생성 성공 테스트`() = runTest {
        // given
        val profileId = 1L
        val request = QuestionGenerationRequest(profileId = profileId)
        
        val profile = createMockProfile()
        val llmResponse = LlmResponse(
            content = createMockLlmResponse(),
            tokensUsed = 1000,
            cost = 0.05,
            provider = "GEMINI"
        )
        
        every { profileService.getProfile(profileId) } returns profile
        every { cacheService.getCached(any()) } returns null
        coEvery { llmProvider.generateResponse(any(), any()) } returns llmResponse
        every { questionSetRepository.save(any()) } returns createMockQuestionSet()
        every { cacheService.cache(any(), any(), any()) } just Runs
        
        // when
        val result = questionGenerationService.generateQuestions(request)
        
        // then
        assertThat(result.questions).hasSize(5)
        assertThat(result.metadata.generationType).isEqualTo(GenerationType.BASIC)
        verify { cacheService.cache(any(), any(), any()) }
    }
    
    @Test
    fun `캐시된 결과 반환 테스트`() = runTest {
        // given
        val request = QuestionGenerationRequest(profileId = 1L)
        val cachedResponse = createMockQuestionSetResponse()
        
        every { profileService.getProfile(1L) } returns createMockProfile()
        every { cacheService.getCached(any()) } returns cachedResponse
        
        // when
        val result = questionGenerationService.generateQuestions(request)
        
        // then
        assertThat(result).isEqualTo(cachedResponse)
        verify { cacheService.getCached(any()) }
        verify(exactly = 0) { llmProvider.generateResponse(any(), any()) }
    }
}
```

---

### 5.8 TASK-012: 프로필 REST API

**브랜치:** `feature/TASK-012-profile-rest-api`

**컨트롤러 구현:**
```kotlin
@RestController
@RequestMapping("/api/v1/profiles")
@Validated
class ProfileController(
    private val profileService: ProfileService
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(ProfileController::class.java)
    }
    
    @PostMapping
    @Operation(summary = "새로운 프로필 생성", description = "이력서 정보를 바탕으로 새로운 프로필을 생성합니다.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "프로필 생성 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        ApiResponse(responseCode = "500", description = "서버 내부 오류")
    ])
    fun createProfile(
        @Valid @RequestBody request: ProfileCreateRequest
    ): ResponseEntity<ProfileResponse> {
        logger.info("프로필 생성 요청: ${request.basicInfo.name}")
        
        val response = profileService.createProfile(request)
        
        logger.info("프로필 생성 완료: ID ${response.id}")
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
    
    @GetMapping("/{profileId}")
    @Operation(summary = "프로필 조회", description = "프로필 ID로 특정 프로필을 조회합니다.")
    fun getProfile(
        @PathVariable 
        @Schema(description = "프로필 ID", example = "1")
        profileId: Long
    ): ResponseEntity<ProfileResponse> {
        logger.info("프로필 조회 요청: ID $profileId")
        
        val response = profileService.getProfile(profileId)
        
        return ResponseEntity.ok(response)
    }
    
    @PutMapping("/{profileId}")
    @Operation(summary = "프로필 수정", description = "기존 프로필 정보를 수정합니다.")
    fun updateProfile(
        @PathVariable profileId: Long,
        @Valid @RequestBody request: ProfileUpdateRequest
    ): ResponseEntity<ProfileResponse> {
        logger.info("프로필 수정 요청: ID $profileId")
        
        val response = profileService.updateProfile(profileId, request)
        
        logger.info("프로필 수정 완료: ID $profileId")
        return ResponseEntity.ok(response)
    }
    
    @DeleteMapping("/{profileId}")
    @Operation(summary = "프로필 삭제", description = "프로필을 삭제합니다.")
    fun deleteProfile(
        @PathVariable profileId: Long
    ): ResponseEntity<Void> {
        logger.info("프로필 삭제 요청: ID $profileId")
        
        profileService.deleteProfile(profileId)
        
        logger.info("프로필 삭제 완료: ID $profileId")
        return ResponseEntity.noContent().build()
    }
}
```

**DTO 클래스들:**
```kotlin
data class ProfileCreateRequest(
    @field:Valid
    @Schema(description = "기본 정보")
    val basicInfo: BasicInfoRequest,
    
    @Schema(description = "경력 정보")
    val experiences: List<ExperienceRequest>? = null,
    
    @Schema(description = "스킬 정보")
    val skills: SkillsRequest? = null,
    
    @Schema(description = "목표 정보")
    val goals: GoalsRequest? = null
)

data class BasicInfoRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(max = 100, message = "이름은 100자를 초과할 수 없습니다")
    @Schema(description = "이름", example = "홍길동")
    val name: String,
    
    @field:Size(max = 50, message = "총 경력은 50자를 초과할 수 없습니다")
    @Schema(description = "총 경력", example = "3년 6개월")
    val totalExperience: String?,
    
    @field:Size(max = 200, message = "현재 직무는 200자를 초과할 수 없습니다")
    @Schema(description = "현재 직무", example = "백엔드 개발자")
    val currentRole: String?,
    
    @field:Size(max = 200, message = "목표 직무는 200자를 초과할 수 없습니다")
    @Schema(description = "목표 직무", example = "시니어 백엔드 개발자")
    val targetRole: String?,
    
    @field:Size(max = 100, message = "목표 업계는 100자를 초과할 수 없습니다")
    @Schema(description = "목표 업계", example = "핀테크")
    val targetIndustry: String?,
    
    @field:Size(max = 100, message = "지역은 100자를 초과할 수 없습니다")
    @Schema(description = "지역", example = "서울")
    val location: String?,
    
    @Schema(description = "근무 형태", example = "HYBRID")
    val workType: WorkType? = WorkType.HYBRID
)

data class ProfileResponse(
    @Schema(description = "프로필 ID", example = "1")
    val id: Long,
    
    @Schema(description = "기본 정보")
    val basicInfo: BasicInfoResponse,

    **ProfileResponse 계속:**
```kotlin
    @Schema(description = "경력 정보")
    val experiences: List<ExperienceResponse> = emptyList(),
    
    @Schema(description = "스킬 정보")
    val skills: SkillsResponse? = null,
    
    @Schema(description = "목표 정보")
    val goals: GoalsResponse? = null,
    
    @Schema(description = "프로필 완성도", example = "0.85")
    val profileCompleteness: Double,
    
    @Schema(description = "생성일시")
    val createdAt: LocalDateTime,
    
    @Schema(description = "수정일시")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(profile: Profile): ProfileResponse {
            return ProfileResponse(
                id = profile.id,
                basicInfo = BasicInfoResponse.from(profile),
                experiences = profile.experiences.map { ExperienceResponse.from(it) },
                skills = SkillsResponse.from(profile),
                goals = GoalsResponse.from(profile),
                profileCompleteness = profile.profileCompleteness,
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt
            )
        }
    }
}
```

**컨트롤러 테스트:**
```kotlin
@WebMvcTest(ProfileController::class)
class ProfileControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @MockK
    private lateinit var profileService: ProfileService
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @Test
    fun `프로필 생성 성공 테스트`() {
        // given
        val request = ProfileCreateRequest(
            basicInfo = BasicInfoRequest(
                name = "홍길동",
                currentRole = "백엔드 개발자"
            )
        )
        
        val response = ProfileResponse(
            id = 1L,
            basicInfo = BasicInfoResponse(name = "홍길동"),
            profileCompleteness = 0.5,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        every { profileService.createProfile(request) } returns response
        
        // when & then
        mockMvc.perform(
            post("/api/v1/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.basicInfo.name").value("홍길동"))
            .andDo(print())
    }
    
    @Test
    fun `프로필 조회 성공 테스트`() {
        // given
        val profileId = 1L
        val response = createMockProfileResponse()
        
        every { profileService.getProfile(profileId) } returns response
        
        // when & then
        mockMvc.perform(get("/api/v1/profiles/{profileId}", profileId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(profileId))
            .andDo(print())
    }
    
    @Test
    fun `존재하지 않는 프로필 조회 시 404 반환`() {
        // given
        val profileId = 999L
        every { profileService.getProfile(profileId) } throws ProfileNotFoundException("프로필을 찾을 수 없습니다")
        
        // when & then
        mockMvc.perform(get("/api/v1/profiles/{profileId}", profileId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("PROFILE_NOT_FOUND"))
    }
    
    @Test
    fun `유효하지 않은 프로필 생성 요청 시 400 반환`() {
        // given
        val invalidRequest = ProfileCreateRequest(
            basicInfo = BasicInfoRequest(name = "") // 빈 이름
        )
        
        // when & then
        mockMvc.perform(
            post("/api/v1/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
    }
}
```

---

### 5.9 TASK-013: 면접 질문 REST API

**브랜치:** `feature/TASK-013-interview-rest-api`

**컨트롤러 구현:**
```kotlin
@RestController
@RequestMapping("/api/v1/interviews")
@Validated
class InterviewController(
    private val questionGenerationService: QuestionGenerationService
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(InterviewController::class.java)
    }
    
    @PostMapping("/questions")
    @Operation(
        summary = "맞춤형 면접 질문 생성",
        description = "프로필 정보를 바탕으로 개인화된 면접 질문을 생성합니다."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "질문 생성 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        ApiResponse(responseCode = "404", description = "프로필을 찾을 수 없음"),
        ApiResponse(responseCode = "500", description = "질문 생성 실패")
    ])
    suspend fun generateQuestions(
        @Valid @RequestBody request: QuestionGenerationRequest
    ): ResponseEntity<QuestionSetResponse> {
        logger.info("질문 생성 요청 - 프로필 ID: ${request.profileId}, 질문 수: ${request.questionCount}")
        
        val startTime = System.currentTimeMillis()
        
        try {
            val response = questionGenerationService.generateQuestions(request)
            
            val duration = System.currentTimeMillis() - startTime
            logger.info("질문 생성 완료 - 소요시간: ${duration}ms, 질문 ID: ${response.id}")
            
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.error("질문 생성 실패 - 소요시간: ${duration}ms", e)
            throw e
        }
    }
    
    @GetMapping("/questions/{questionSetId}")
    @Operation(
        summary = "생성된 질문 세트 조회",
        description = "이전에 생성된 면접 질문 세트를 조회합니다."
    )
    fun getQuestionSet(
        @PathVariable 
        @Schema(description = "질문 세트 ID", example = "1")
        questionSetId: Long
    ): ResponseEntity<QuestionSetResponse> {
        logger.info("질문 세트 조회 요청: ID $questionSetId")
        
        val response = questionGenerationService.getQuestionSet(questionSetId)
        
        return ResponseEntity.ok(response)
    }
    
    @PostMapping("/feedback")
    @Operation(
        summary = "질문 품질 피드백 제출",
        description = "생성된 질문에 대한 사용자 피드백을 제출합니다."
    )
    fun submitQuestionFeedback(
        @Valid @RequestBody request: QuestionFeedbackRequest
    ): ResponseEntity<Void> {
        logger.info("질문 피드백 제출 - 질문 세트 ID: ${request.questionSetId}")
        
        questionGenerationService.submitFeedback(request)
        
        return ResponseEntity.ok().build()
    }
}
```

**DTO 클래스들:**
```kotlin
data class QuestionGenerationRequest(
    @field:Positive(message = "프로필 ID는 양수여야 합니다")
    @Schema(description = "프로필 ID", example = "1")
    val profileId: Long,
    
    @field:Min(value = 3, message = "최소 3개의 질문이 필요합니다")
    @field:Max(value = 10, message = "최대 10개의 질문까지 생성 가능합니다")
    @Schema(description = "생성할 질문 수", example = "5")
    val questionCount: Int = 5,
    
    @Schema(description = "질문 난이도", example = "INTERMEDIATE")
    val difficulty: QuestionDifficulty? = null,
    
    @field:Size(max = 200, message = "목표 회사명은 200자를 초과할 수 없습니다")
    @Schema(description = "목표 회사명", example = "카카오")
    val targetCompany: String? = null,
    
    @Schema(description = "집중할 영역", example = "[\"기술역량\", \"리더십\"]")
    val focusAreas: List<String> = emptyList()
)

data class QuestionFeedbackRequest(
    @field:Positive(message = "질문 세트 ID는 양수여야 합니다")
    @Schema(description = "질문 세트 ID", example = "1")
    val questionSetId: Long,
    
    @Schema(description = "피드백 유형", example = "QUALITY")
    val feedbackType: FeedbackType,
    
    @field:Min(value = 1, message = "평점은 1 이상이어야 합니다")
    @field:Max(value = 10, message = "평점은 10 이하여야 합니다")
    @Schema(description = "평점 (1-10)", example = "8")
    val rating: Int,
    
    @field:Size(max = 1000, message = "코멘트는 1000자를 초과할 수 없습니다")
    @Schema(description = "피드백 코멘트", example = "질문이 실무와 잘 연결되어 도움이 되었습니다.")
    val comment: String? = null
)

enum class FeedbackType {
    QUALITY,        // 질문 품질
    USEFULNESS,     // 유용성
    DIFFICULTY,     // 난이도 적절성
    PERSONALIZATION // 개인화 수준
}
```

**비동기 처리를 위한 설정:**
```kotlin
@Configuration
@EnableAsync
class AsyncConfig {
    
    @Bean
    fun taskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 4
        executor.maxPoolSize = 8
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("async-")
        executor.initialize()
        return executor
    }
}
```

**컨트롤러 테스트:**
```kotlin
@WebMvcTest(InterviewController::class)
class InterviewControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @MockK
    private lateinit var questionGenerationService: QuestionGenerationService
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @Test
    fun `질문 생성 성공 테스트`() = runTest {
        // given
        val request = QuestionGenerationRequest(
            profileId = 1L,
            questionCount = 5
        )
        
        val response = QuestionSetResponse(
            id = 1L,
            profileId = 1L,
            questions = createMockQuestions(),
            metadata = QuestionSetMetadata(
                generationType = GenerationType.BASIC,
                personalizationLevel = 0.8,
                generationCost = 0.05,
                cacheUtilized = false
            ),
            createdAt = LocalDateTime.now()
        )
        
        coEvery { questionGenerationService.generateQuestions(request) } returns response
        
        // when & then
        mockMvc.perform(
            post("/api/v1/interviews/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.questions").isArray)
            .andExpect(jsonPath("$.questions", hasSize(5)))
            .andExpect(jsonPath("$.metadata.personalizationLevel").value(0.8))
            .andDo(print())
    }
    
    @Test
    fun `질문 생성 요청 유효성 검증 테스트`() {
        // given
        val invalidRequest = QuestionGenerationRequest(
            profileId = -1L, // 잘못된 프로필 ID
            questionCount = 15 // 최대 허용 수 초과
        )
        
        // when & then
        mockMvc.perform(
            post("/api/v1/interviews/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.details").exists())
    }
}
```

---

### 5.10 TASK-014: 학습 경로 REST API

**브랜치:** `feature/TASK-014-learning-rest-api`

**학습 경로 서비스 구현:**
```kotlin
@Service
@Transactional
class LearningPathService(
    private val profileService: ProfileService,
    private val llmProvider: LlmProvider,
    private val roadmapRepository: LearningRoadmapRepository,
    private val marketDataProvider: MarketDataProvider
) {
    
    companion object {
        private val logger = LoggerFactory.getLogger(LearningPathService::class.java)
    }
    
    suspend fun generateLearningRoadmap(request: LearningRoadmapRequest): LearningRoadmapResponse {
        logger.info("학습 경로 생성 시작 - 프로필 ID: ${request.profileId}")
        
        // 1. 프로필 조회
        val profile = profileService.getProfile(request.profileId)
        
        // 2. 스킬 갭 분석
        val skillGapAnalysis = analyzeSkillGaps(profile)
        
        // 3. 시장 트렌드 반영
        val marketTrends = marketDataProvider.getCurrentTrends(profile.basicInfo.targetRole)
        
        // 4. 학습 경로 생성 프롬프트 구성
        val prompt = buildLearningPathPrompt(profile, skillGapAnalysis, marketTrends, request)
        
        // 5. LLM 호출
        val llmResponse = llmProvider.generateResponse(prompt, LlmConfig(maxTokens = 4000))
        
        // 6. 응답 파싱
        val roadmapData = parseLearningRoadmapFromResponse(llmResponse.content)
        
        // 7. 데이터베이스 저장
        val savedRoadmap = saveLearningRoadmap(profile, roadmapData, request)
        
        logger.info("학습 경로 생성 완료 - 로드맵 ID: ${savedRoadmap.id}")
        return LearningRoadmapResponse.from(savedRoadmap)
    }
    
    private fun analyzeSkillGaps(profile: ProfileResponse): SkillGapAnalysis {
        val targetRole = profile.basicInfo.targetRole ?: profile.basicInfo.currentRole ?: ""
        val currentSkills = profile.skills?.technical?.map { it.name }?.toSet() ?: emptySet()
        
        // 목표 직무별 필수 스킬 정의 (향후 데이터베이스로 이동)
        val requiredSkills = getRequiredSkillsForRole(targetRole)
        
        val missingSkills = requiredSkills - currentSkills
        val needsImprovement = profile.skills?.technical
            ?.filter { it.proficiency in listOf("BEGINNER", "INTERMEDIATE") }
            ?.map { it.name }?.toSet() ?: emptySet()
        
        return SkillGapAnalysis(
            missingSkills = missingSkills.toList(),
            needsImprovement = needsImprovement.toList(),
            strengths = currentSkills.intersect(requiredSkills).toList()
        )
    }
    
    private fun buildLearningPathPrompt(
        profile: ProfileResponse,
        skillGaps: SkillGapAnalysis,
        marketTrends: MarketTrends,
        request: LearningRoadmapRequest
    ): String {
        return """
            당신은 IT 업계의 경험 많은 커리어 컨설턴트입니다.
            다음 정보를 바탕으로 구체적이고 실행 가능한 학습 로드맵을 생성하세요.

            [현재 상황]
            - 이름: ${profile.basicInfo.name}
            - 현재 직무: ${profile.basicInfo.currentRole}
            - 목표 직무: ${profile.basicInfo.targetRole}
            - 총 경력: ${profile.basicInfo.totalExperience}
            - 현재 스킬: ${profile.skills?.technical?.joinToString { "${it.name}(${it.proficiency})" }}

            [스킬 갭 분석]
            - 부족한 스킬: ${skillGaps.missingSkills.joinToString()}
            - 개선 필요 스킬: ${skillGaps.needsImprovement.joinToString()}
            - 강점 스킬: ${skillGaps.strengths.joinToString()}

            [시장 트렌드]
            - 인기 상승 기술: ${marketTrends.risingSkills.joinToString()}
            - 중요도 높은 스킬: ${marketTrends.highDemandSkills.joinToString()}

            [개인 제약사항]
            - 가용 학습 시간: ${request.availableHoursPerWeek}시간/주
            - 예산: ${request.budget}만원/월
            - 선호 학습 방식: ${request.preferredLearningStyle}

            다음 JSON 형식으로 3단계 학습 로드맵을 생성하세요:

            {
              "analysis": {
                "currentLevel": "현재 수준 평가",
                "targetLevel": "목표 수준",
                "timeToGoal": "목표 달성 예상 기간",
                "marketFit": "시장 적합도 (0.0-1.0)"
              },
              "skillGaps": [
                {
                  "category": "스킬 카테고리",
                  "importance": "HIGH|MEDIUM|LOW",
                  "currentLevel": "1-10 점수",
                  "targetLevel": "1-10 점수",
                  "marketDemand": "VERY_HIGH|HIGH|MEDIUM|LOW"
                }
              ],
              "roadmap": {
                "phase1": {
                  "name": "1단계 이름",
                  "duration": "3개월",
                  "goal": "단계 목표",
                  "priority": "HIGH|MEDIUM|LOW",
                  "tasks": [
                    {
                      "name": "태스크 이름",
                      "type": "LEARNING|PROJECT|PRACTICE",
                      "estimatedHours": 40,
                      "deadline": "4주",
                      "successCriteria": "성공 기준",
                      "resources": [
                        {
                          "type": "COURSE|BOOK|VIDEO|ARTICLE",
                          "title": "리소스 제목",
                          "provider": "제공자",
                          "cost": 77000,
                          "rating": 4.8,
                          "url": "URL (선택사항)"
                        }
                      ],
                      "deliverables": ["산출물1", "산출물2"]
                    }
                  ]
                },
                "phase2": { /* 6개월 계획 */ },
                "phase3": { /* 12개월 계획 */ }
              }
            }

            반드시 유효한 JSON 형식으로만 응답하세요.
        """.trimIndent()
    }
    
    private fun parseLearningRoadmapFromResponse(content: String): LearningRoadmapData {
        return try {
            val cleanContent = content.replace("```json", "").replace("```", "").trim()
            objectMapper.readValue(cleanContent, LearningRoadmapData::class.java)
        } catch (e: Exception) {
            logger.error("학습 로드맵 파싱 실패: $content", e)
            throw LearningRoadmapException("LLM 응답 파싱에 실패했습니다", e)
        }
    }
}
```

**컨트롤러 구현:**
```kotlin
@RestController
@RequestMapping("/api/v1/learning")
@Validated
class LearningController(
    private val learningPathService: LearningPathService
) {
    
    @PostMapping("/roadmap")
    @Operation(
        summary = "개인화된 학습 경로 생성",
        description = "프로필 정보와 개인 제약사항을 바탕으로 맞춤형 학습 로드맵을 생성합니다."
    )
    suspend fun generateLearningRoadmap(
        @Valid @RequestBody request: LearningRoadmapRequest
    ): ResponseEntity<LearningRoadmapResponse> {
        val response = learningPathService.generateLearningRoadmap(request)
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/roadmap/{roadmapId}")
    @Operation(summary = "학습 로드맵 조회")
    fun getLearningRoadmap(
        @PathVariable roadmapId: Long
    ): ResponseEntity<LearningRoadmapResponse> {
        val response = learningPathService.getRoadmap(roadmapId)
        return ResponseEntity.ok(response)
    }
    
    @PutMapping("/progress")
    @Operation(summary = "학습 진도 업데이트")
    fun updateLearningProgress(
        @Valid @RequestBody request: LearningProgressUpdateRequest
    ): ResponseEntity<Void> {
        learningPathService.updateProgress(request)
        return ResponseEntity.ok().build()
    }
}
```

---

### 5.11 TASK-015: 단위 테스트 작성

**브랜치:** `feature/TASK-015-unit-tests`

**테스트 전략:**
1. **Service Layer**: 비즈니스 로직 검증
2. **Repository Layer**: 데이터 접근 로직 검증
3. **Controller Layer**: API 입출력 검증
4. **External Integration**: 외부 API 연동 검증

**테스트 기본 설정:**
```kotlin
// 테스트 기본 클래스
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class BaseIntegrationTest {
    
    @Autowired
    protected lateinit var testEntityManager: TestEntityManager
    
    @BeforeAll
    fun setup() {
        // 테스트 데이터 초기화
    }
    
    @AfterEach
    fun cleanup() {
        // 테스트 데이터 정리
    }
}

// application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  
  flyway:
    enabled: false

llm:
  provider: MOCK
```

**Service 테스트 예시:**
```kotlin
@ExtendWith(MockKExtension::class)
class QuestionGenerationServiceTest {
    
    @MockK
    private lateinit var profileService: ProfileService
    
    @MockK
    private lateinit var llmProvider: LlmProvider
    
    @MockK
    private lateinit var cacheService: CacheService
    
    @MockK
    private lateinit var questionSetRepository: InterviewQuestionSetRepository
    
    @InjectMockKs
    private lateinit var questionGenerationService: QuestionGenerationService
    
    @Test
    fun `정상적인 프로필로 질문 생성 성공`() = runTest {
        // given
        val profileId = 1L
        val request = QuestionGenerationRequest(profileId = profileId)
        val profile = createValidProfile()
        val llmResponse = createValidLlmResponse()
        val expectedQuestionSet = createExpectedQuestionSet()
        
        every { profileService.getProfile(profileId) } returns profile
        every { cacheService.getCached(any()) } returns null
        coEvery { llmProvider.generateResponse(any(), any()) } returns llmResponse
        every { questionSetRepository.save(any()) } returns expectedQuestionSet
        every { cacheService.cache(any(), any(), any()) } just Runs
        
        // when
        val result = questionGenerationService.generateQuestions(request)
        
        // then
        assertThat(result.questions).hasSize(5)
        assertThat(result.profileId).isEqualTo(profileId)
        assertThat(result.metadata.generationType).isEqualTo(GenerationType.BASIC)
        
        verify { profileService.getProfile(profileId) }
        verify { cacheService.getCached(any()) }
        coVerify { llmProvider.generateResponse(any(), any()) }
        verify { questionSetRepository.save(any()) }
        verify { cacheService.cache(any(), any(), any()) }
    }
    
    @Test
    fun `캐시된 결과가 있을 때 LLM 호출하지 않음`() = runTest {
        // given
        val request = QuestionGenerationRequest(profileId = 1L)
        val profile = createValidProfile()
        val cachedResponse = createCachedQuestionSetResponse()
        
        every { profileService.getProfile(1L) } returns profile
        every { cacheService.getCached(any()) } returns cachedResponse
        
        // when
        val result = questionGenerationService.generateQuestions(request)
        
        // then
        assertThat(result).isEqualTo(cachedResponse)
        
        verify { profileService.getProfile(1L) }
        verify { cacheService.getCached(any()) }
        coVerify(exactly = 0) { llmProvider.generateResponse(any(), any()) }
    }
    
    @Test
    fun `존재하지 않는 프로필로 요청 시 예외 발생`() = runTest {
        // given
        val profileId = 999L
        val request = QuestionGenerationRequest(profileId = profileId)
        
        every { profileService.getProfile(profileId) } throws ProfileNotFoundException("프로필을 찾을 수 없습니다")
        
        // when & then
        assertThrows<ProfileNotFoundException> {
            runBlocking { questionGenerationService.generateQuestions(request) }
        }
        
        verify { profileService.getProfile(profileId) }
        coVerify(exactly = 0) { llmProvider.generateResponse(any(), any()) }
    }
    
    @Test
    fun `LLM API 호출 실패 시 예외 발생`() = runTest {
        // given
        val request = QuestionGenerationRequest(profileId = 1L)
        val profile = createValidProfile()
        
        every { profileService.getProfile(1L) } returns profile
        every { cacheService.getCached(any()) } returns null
        coEvery { llmProvider.generateResponse(any(), any()) } throws RuntimeException("API 호출 실패")
        
        // when & then
        assertThrows<QuestionGenerationException> {
            runBlocking { questionGenerationService.generateQuestions(request) }
        }
    }
    
    private fun createValidProfile(): ProfileResponse {
        return ProfileResponse(
            id = 1L,
            basicInfo = BasicInfoResponse(
                name = "홍길동",
                totalExperience = "3년",
                currentRole = "백엔드 개발자",
                targetRole = "시니어 백엔드 개발자"
            ),
            skills = SkillsResponse(
                technical = listOf(
                    TechnicalSkillResponse(
                        name = "Java",
                        proficiency = "ADVANCED",
                        experienceMonths = 36
                    )
                )
            ),
            profileCompleteness = 0.8,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}
```

---

### 5.12 TASK-016: 통합 테스트 작성

**브랜치:** `feature/TASK-016-integration-tests`

**통합 테스트 구조:**
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
class ProfileIntegrationTest {
    
    @Container
    companion object {
        @JvmStatic
        val postgresql = PostgreSQLContainer<Nothing>("postgres:15").apply {
            withDatabaseName("test_career_coach")
            withUsername("test_user")
            withPassword("test_password")
        }
    }
    
    @Autowired
    private lateinit var testRestTemplate: TestRestTemplate
    
    @Autowired
    private lateinit var profileRepository: ProfileRepository
    
    @DynamicPropertySource
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresql::getJdbcUrl)
            registry.add("spring.datasource.username", postgresql::getUsername)
            registry.add("spring.datasource.password", postgresql::getPassword)
        }
    }
    
    @Test
    fun `프로필 생성부터 조회까지 전체 플로우 테스트`() {
        // given
        val createRequest = ProfileCreateRequest(
            basicInfo = BasicInfoRequest(
                name = "통합테스트사용자",
                currentRole = "백엔드 개발자",
                targetRole = "시니어 백엔드 개발자"
            ),
            experiences = listOf(
                ExperienceRequest(
                    company = "테스트회사",
                    position = "개발자",
                    duration = "2년"
                )
            )
        )
        
        // when 1: 프로필 생성
        val createResponse = testRestTemplate.postForEntity(
            "/api/v1/profiles",
            createRequest,
            ProfileResponse::class.java
        )
        
        // then 1: 생성 성공
        assertThat(createResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(createResponse.body?.basicInfo?.name).isEqualTo("통합테스트사용자")
        
        val profileId = createResponse.body?.id!!
        
        // when 2: 프로필 조회
        val getResponse = testRestTemplate.getForEntity(
            "/api/v1/profiles/$profileId",
            ProfileResponse::class.java
        )
        
        // then 2: 조회 성공
        assertThat(getResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(getResponse.body?.id).isEqualTo(profileId)
        assertThat(getResponse.body?.experiences).hasSize(1)
        
        // when 3: 데이터베이스 직접 확인
        val profileEntity = profileRepository.findById(profileId)
        
        // then 3: 데이터베이스에 정상 저장
        assertThat(profileEntity).isPresent
        assertThat(profileEntity.get().name).isEqualTo("통합테스트사용자")
    }
    
    @Test
    fun `존재하지 않는 프로필 조회 시 404 응답`() {
        // when
        val response = testRestTemplate.getForEntity(
            "/api/v1/profiles/999999",
            ErrorResponse::class.java
        )
        
        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body?.error?.code).isEqualTo("PROFILE_NOT_FOUND")
    }
}
```

**End-to-End 테스트:**
```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EndToEndWorkflowTest {
    
    @Test
    fun `프로필 생성 후 질문 생성까지 전체 워크플로우 테스트`() {
        // 1. 프로필 생성
        val profileResponse = createTestProfile()
        val profileId = profileResponse.id
        
        // 2. 질문 생성 요청
        val questionRequest = QuestionGenerationRequest(
            profileId = profileId,
            questionCount = 5,
            difficulty = QuestionDifficulty.INTERMEDIATE
        )
        
        val questionResponse = testRestTemplate.postForEntity(
            "/api/v1/interviews/questions",
            questionRequest,
            QuestionSetResponse::class.java
        )
        
        // 3. 검증
        assertThat(questionResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(questionResponse.body?.questions).hasSize(5)
        assertThat(questionResponse.body?.profileId).isEqualTo(profileId)
        
        // 4. 생성된 질문 조회
        val questionSetId = questionResponse.body?.id!!
        val getQuestionResponse = testRestTemplate.getForEntity(
            "/api/v1/interviews/questions/$questionSetId",
            QuestionSetResponse::class.java
        )
        
        assertThat(getQuestionResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(getQuestionResponse.body?.questions).hasSize(5)
    }
    
    @Test
    fun `학습 로드맵 생성 전체 워크플로우 테스트`() {
        // 1. 프로필 생성
        val profileResponse = createTestProfile()
        
        // 2. 학습 로드맵 생성
        val roadmapRequest = LearningRoadmapRequest(
            profileId = profileResponse.id,
            availableHoursPerWeek = 10,
            budget = 20,
            preferredLearningStyle = "실습 중심"
        )
        
        val roadmapResponse = testRestTemplate.postForEntity(
            "/api/v1/learning/roadmap",
            roadmapRequest,
            LearningRoadmapResponse::class.java
        )
        
        // 3. 검증
        assertThat(roadmapResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(roadmapResponse.body?.roadmap?.phase1).isNotNull
        assertThat(roadmapResponse.body?.skillGaps).isNotEmpty
    }
}
```

---

### 5.13 TASK-017: API 문서화

**브랜치:** `feature/TASK-017-api-documentation`

**OpenAPI 설정:**
```kotlin
@Configuration
class OpenApiConfig {
    
    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Career Coach API")
                    .description("이력서 기반 개인 맞춤형 커리어 코치 챗봇 API")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("Career Coach Team")
                            .email("support@careercoach.com")
                    )
                    .license(
                        License()
                            .name("MIT")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8080")
                        .description("개발 서버"),
                    Server()
                        .url("https://api.careercoach.com")
                        .description("운영 서버")
                )
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )
    }
}
```

**API 문서 자동 생성을 위한 어노테이션 강화:**
```kotlin
@Tag(name = "Profile", description = "프로필 관리 API")
@RestController
@RequestMapping("/api/v1/profiles")
class ProfileController {
    
    @Operation(
        summary = "프로필 생성",
        description = """
            새로운 사용자 프로필을 생성합니다.
            
            ### 주요 기능
            - 기본 정보, 경력, 스킬, 목표 정보 저장
            - 프로필 완성도 자동 계산
            - 유효성 검증
            
            ### 요청 예시
            ```json
            {
              "basicInfo": {
                "name": "홍길동",
                "currentRole": "백엔드 개발자",
                "targetRole": "시니어 백엔드 개발자"
              }
            }
            ```
        """,
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "프로필 생성 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ProfileResponse::class),
                    examples = [ExampleObject(
                        name = "성공 응답 예시",
                        value = """
                            {
                              "id": 1,
                              "basicInfo": {
                                "name": "홍길동",
                                "currentRole": "백엔드 개발자"
                              },
                              "profileCompleteness": 0.65,
                              "createdAt": "2024-08-20T10:30:00"
                            }
                        """
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400", 
                description = "잘못된 요청 데이터",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class)
                )]
            )
        ]
    )
    @PostMapping
    fun createProfile(@Valid @RequestBody request: ProfileCreateRequest): ResponseEntity<ProfileResponse> {
        // 구현
    }
}
```

**README.md 업데이트:**
```markdown
# Career Coach API

이력서 기반 개인 맞춤형 커리어 코치 챗봇 API

## 🚀 빠른 시작

### 환경 요구사항
- Java 17+
- Docker & Docker Compose
- Kotlin 1.9+

### 로컬 개발 환경 설정

1. 저장소 클론
```bash
git clone <repository-url>
cd career-coach-api
```

2. 환경 변수 설정
```bash
cp .env.example .env
# .env 파일에서 GEMINI_API_KEY 설정
```

3. 데이터베이스 실행
```bash
docker-compose -f docker/docker-compose.yml up -d
```

4. 애플리케이션 실행
```bash
./gradlew bootRun
```

### API 문서 확인
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 📋 주요 기능

### 1. 프로필 관리
- ✅ 프로필 생성/수정/조회/삭제
- ✅ 경력 정보 관리
- ✅ 기술 스킬 관리
- ✅ 프로필 완성도 자동 계산

### 2. 면접 질문 생성
- ✅ AI 기반 맞춤형 질문 생성
- ✅ 난이도별 질문 분류
- ✅ 질문 의도 및 평가 포인트 제공
- ✅ 스마트 캐싱으로 비용 절감

### 3. 학습 경로 추천
- ✅ 스킬 갭 분석
- ✅ 단계별 학습 계획 생성
- ✅ 개인 제약사항 반영
- ✅ 구체적 리소스 추천

## 🧪 테스트

```bash
# 단위 테스트
./gradlew test

# 통합 테스트
./gradlew integrationTest

# 전체 테스트
./gradlew check
```

## 📊 모니터링

### Health Check
- GET /actuator/health

### Metrics
- GET /actuator/metrics

## 🔧 개발 가이드

### 브랜치 전략
- `main`: 운영 배포 브랜치
- `develop`: 개발 통합 브랜치
- `feature/TASK-XXX-description`: 기능 개발 브랜치

### 커밋 컨벤션
```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
test: 테스트 코드 추가
refactor: 코드 리팩토링
```

### 코드 스타일
- Kotlin 공식 코딩 컨벤션 준수
- ktlint를 사용한 자동 포맷팅

## 📈 로드맵

### Phase 1 (완료)
- ✅ 기본 API 구현
- ✅ 프로필 관리
- ✅ 질문 생성
- ✅ 학습 경로 추천

### Phase 2 (예정)
- 🔄 Multi-Agent 시스템
- 🔄 지능형 캐싱
- 🔄 실시간 면접 시뮬레이션

### Phase 3 (예정)
- ⭕ 예측 모델
- ⭕ 적응형 시스템
- ⭕ 고급 개인화
```

## 6. 테스트 전략

### 6.1 테스트 피라미드
```
    🔺 E2E Tests (적음)
   🔺🔺 Integration Tests (보통)
  🔺🔺🔺 Unit Tests (많음)
```

### 6.2 각 레이어별 테스트 가이드

**단위 테스트 (70%)**
- Service 로직 검증
- Util 클래스 검증
- 도메인 객체 검증

**통합 테스트 (20%)**
- Repository 테스트
- Controller 테스트
- 외부 API 연동 테스트

**E2E 테스트 (10%)**
- 전체 워크플로우 검증
- 핵심 시나리오 검증

### 6.3 테스트 실행 명령어
```bash
# 단위 테스트만 실행
./gradlew test

# 통합 테스트만 실행  
./gradlew integrationTest

# 전체 테스트 실행
./gradlew check

# 특정 테스트 클래스 실행
./gradlew test --tests ProfileServiceTest

# 테스트 커버리지 리포트 생성
./gradlew jacocoTestReport
```

## 7. 배포 및 운영

### 7.1 배포 준비사항
1. **환경 변수 설정 확인**
2. **데이터베이스 마이그레이션 실행**
3. **Health Check 엔드포인트 동작 확인**
4. **모니터링 설정**

### 7.2 배포 체크리스트
```markdown
## 배포 전 체크리스트
- [ ] 모든 테스트 통과
- [ ] 환경 변수 설정 완료
- [ ] 데이터베이스 마이그레이션 준비
- [ ] API 문서 업데이트
- [ ] 로그 레벨 설정 확인
- [ ] 모니터링 설정 확인

## 배포 후 체크리스트  
- [ ] Health Check 정상 응답
- [ ] API 엔드포인트 동작 확인
- [ ] 데이터베이스 연결 확인
- [ ] LLM API 연동 확인
- [ ] 로그 정상 출력 확인
```

---

## 마무리

이 구현 가이드를 통해 개발자는:

1. **체계적인 개발 프로세스**를 따라 단계별로 구현
2. **품질 높은 코드**를 작성하고 충분한 테스트 커버리지 확보
3. **확장 가능한 아키텍처**로 향후 고도화 준비
4. **완전한 API 문서화**로 사용성 극대화

각 Task별로 브랜치를 생성하고, 완료 후 테스트를 거쳐 통합하는 과정을 통해 **안정적이고 견고한 MVP**를 구축할 수 있습니다.