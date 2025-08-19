# 커리어 코치 챗봇 API 구현 가이드

## 1. 프로젝트 초기 설정

### 1.1 기본 구조
```
career-coach-api/
├── src/main/kotlin/com/careercoach/
├── src/main/resources/
├── src/test/kotlin/
├── docker/
├── docs/
├── build.gradle.kts
└── README.md
```

### 1.2 기술 스택
- **Backend**: Kotlin + Spring Boot 3.x
- **Database**: PostgreSQL (Docker)
- **LLM**: Google Gemini API
- **Documentation**: OpenAPI 3.0

### 1.3 의존성
- Spring Boot Web, JPA, Validation, Actuator
- PostgreSQL Driver, Flyway
- Jackson Kotlin Module
- WebFlux (HTTP Client)
- OpenAPI Docs
- Test: JUnit, MockK, Testcontainers

## 2. 개발 워크플로우

### 2.1 Task 관리
**docs/tasks/task-tracker.md** 파일로 진행상황 추적
- 각 task별 체크리스트
- 완료된 task 기록
- 현재 진행 중인 작업 표시

### 2.2 브랜치 전략
```
main (운영)
├── develop (개발 통합)
    ├── feature/TASK-001-project-setup
    ├── feature/TASK-002-database-setup
    └── feature/TASK-XXX-description
```

### 2.3 커밋 컨벤션
- `feat: TASK-XXX - 기능 설명`
- `fix: 버그 수정`
- `docs: 문서 업데이트`
- `test: 테스트 추가`

## 3. 데이터베이스 설계

### 3.1 핵심 테이블
- **profiles**: 기본 프로필 정보
- **experiences**: 경력 정보 (1:N)
- **projects**: 프로젝트 정보 (1:N)
- **technical_skills**: 기술 스킬 (1:N)
- **interview_question_sets**: 질문 세트
- **interview_questions**: 개별 질문 (1:N)
- **learning_roadmaps**: 학습 로드맵

### 3.2 관계 설계
- Profile → Experiences (1:N, CASCADE)
- Experience → Projects (1:N, CASCADE)
- Profile → Skills (1:N, CASCADE)
- Profile → QuestionSets (1:N)
- QuestionSet → Questions (1:N, CASCADE)

### 3.3 Docker 설정
PostgreSQL 컨테이너 설정으로 개발 환경 표준화

## 4. 아키텍처 설계

### 4.1 레이어 구조
```
Controller Layer (REST API)
    ↓
Service Layer (비즈니스 로직)
    ↓
Repository Layer (데이터 접근)
    ↓
External Layer (LLM API 연동)
```

### 4.2 패키지 구조
```
com.careercoach/
├── config/         # 설정 클래스
├── controller/     # REST 컨트롤러
├── service/        # 비즈니스 서비스
├── domain/         # JPA 엔티티
├── repository/     # 데이터 리포지토리
├── dto/           # 요청/응답 DTO
├── external/      # 외부 API 연동
└── common/        # 공통 유틸리티
```

### 4.3 핵심 설계 원칙
- **단일 책임**: 각 클래스는 하나의 책임만
- **의존성 역전**: 인터페이스 기반 설계
- **확장성**: 새로운 LLM 제공자 추가 용이
- **테스트 가능성**: Mock 테스트 지원

## 5. Phase 1 개발 Tasks

### TASK-001: 프로젝트 초기 설정
**목표**: Spring Boot 프로젝트 생성 및 기본 설정
**결과물**: 
- 실행 가능한 Spring Boot 애플리케이션
- Docker Compose 환경
- 기본 설정 파일들

**완료 조건**: 
- 애플리케이션 정상 실행
- Swagger UI 접근 가능
- 데이터베이스 연결 성공

### TASK-002: 데이터베이스 설정
**목표**: PostgreSQL 설정 및 스키마 생성
**결과물**:
- Flyway 마이그레이션 스크립트
- JPA 기본 설정
- 데이터베이스 연결 테스트

**완료 조건**:
- 모든 테이블 생성 완료
- JPA Auditing 동작 확인

### TASK-003: JPA Entity 구현
**목표**: 도메인 엔티티 클래스 작성
**주의사항**:
- 지연 로딩(LAZY) 기본 사용
- 양방향 관계 시 무한 참조 방지
- Cascade 타입 신중하게 선택

**완료 조건**:
- 모든 엔티티 클래스 구현
- 연관관계 매핑 테스트 통과

### TASK-004: Repository 구현
**목표**: 데이터 접근 계층 구현
**결과물**:
- JPA Repository 인터페이스들
- 커스텀 쿼리 메서드
- Repository 테스트

### TASK-005: LLM 연동 기반 구조
**목표**: 확장 가능한 LLM 통합 구조
**설계 요소**:
- LlmProvider 인터페이스
- GeminiProvider 구현체
- LlmProviderFactory (확장성)
- 설정 기반 제공자 선택

**완료 조건**:
- Gemini API 연동 성공
- 기본 응답 생성 테스트 통과

### TASK-006: 프로필 관리 서비스
**목표**: 프로필 CRUD 비즈니스 로직
**핵심 기능**:
- 프로필 생성/수정/조회/삭제
- 프로필 완성도 자동 계산
- 연관 데이터 처리 (경력, 스킬 등)

**완료 조건**:
- 모든 CRUD 기능 동작
- 비즈니스 규칙 적용
- 단위 테스트 통과

### TASK-007: 질문 생성 서비스
**목표**: AI 기반 맞춤형 면접 질문 생성
**핵심 로직**:
- 프로필 기반 프롬프트 생성
- LLM 호출 및 응답 파싱
- 질문 품질 검증
- 캐싱 전략 적용

**완료 조건**:
- 개인화된 질문 5개 생성
- JSON 응답 파싱 성공
- 에러 핸들링 완료

### TASK-008: 학습 경로 서비스
**목표**: 개인화된 학습 로드맵 생성
**핵심 기능**:
- 스킬 갭 분석
- 단계별 학습 계획 생성
- 구체적 리소스 추천
- 개인 제약사항 반영

### TASK-009: REST API 구현
**목표**: HTTP API 엔드포인트 구현
**API 그룹**:
- 프로필 관리: `/api/v1/profiles`
- 면접 질문: `/api/v1/interviews`
- 학습 경로: `/api/v1/learning`

**완료 조건**:
- 모든 엔드포인트 동작
- 유효성 검증 적용
- 에러 응답 표준화

### TASK-010: 테스트 작성
**테스트 범위**:
- 단위 테스트: Service, Repository 로직
- 통합 테스트: API 엔드포인트, 데이터베이스
- E2E 테스트: 전체 워크플로우

**목표 커버리지**: 80% 이상

### TASK-011: API 문서화
**목표**: 완전한 API 문서 제공
**결과물**:
- OpenAPI 3.0 스펙
- Swagger UI 설정
- API 사용 가이드
- 예제 요청/응답

## 6. 핵심 구현 포인트

### 6.1 LLM 통합 전략
- **인터페이스 기반**: 여러 제공자 지원 준비
- **에러 핸들링**: API 장애 시 Fallback
- **비용 최적화**: 스마트 캐싱 적용
- **응답 검증**: JSON 파싱 및 품질 체크

### 6.2 캐싱 전략
- **레벨 1**: 완전 일치 (24시간)
- **레벨 2**: 높은 유사도 85% (12시간)
- **레벨 3**: 부분 일치 70% (6시간)
- **캐시 키**: 프로필 해시 + 요청 파라미터

### 6.3 에러 처리
- **사용자 정의 예외**: 도메인별 예외 클래스
- **글로벌 핸들러**: @ControllerAdvice 적용
- **표준 응답**: 일관된 에러 응답 형식
- **로깅**: 디버깅을 위한 상세 로그

### 6.4 성능 최적화
- **비동기 처리**: Kotlin Coroutines 활용
- **N+1 방지**: 적절한 fetch join 전략
- **커넥션 풀**: HikariCP 설정
- **인덱스**: 주요 검색 컬럼 인덱싱

## 7. 테스트 전략

### 7.1 테스트 피라미드
```
E2E (10%) - 전체 워크플로우
Integration (20%) - API + DB
Unit (70%) - 비즈니스 로직
```

### 7.2 테스트 도구
- **Unit**: JUnit 5 + MockK
- **Integration**: @SpringBootTest + TestContainers
- **E2E**: TestRestTemplate + 실제 DB

### 7.3 테스트 실행
```bash
./gradlew test              # 단위 테스트
./gradlew integrationTest   # 통합 테스트  
./gradlew check            # 전체 테스트
```

## 8. 배포 준비

### 8.1 환경 설정
- **개발**: application-dev.yml
- **테스트**: application-test.yml  
- **운영**: application-prod.yml

### 8.2 모니터링
- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **로깅**: Logback 설정

### 8.3 보안
- **환경 변수**: API 키 등 민감 정보
- **CORS**: 필요한 도메인만 허용
- **Validation**: 모든 입력 검증

## 9. 주요 유의사항

### 9.1 개발 시 주의점
- **브랜치 관리**: task별 독립 브랜치
- **테스트 우선**: 기능 구현 후 반드시 테스트
- **커밋 단위**: 의미있는 단위로 커밋
- **문서 업데이트**: 변경사항 즉시 반영

### 9.2 코드 품질
- **코딩 컨벤션**: Kotlin 공식 가이드 준수
- **네이밍**: 의도가 드러나는 명명
- **주석**: 왜(Why)를 설명하는 주석
- **리팩토링**: 지속적인 코드 개선

### 9.3 성능 고려사항
- **데이터베이스**: 적절한 인덱스 설계
- **메모리**: 대용량 데이터 처리 주의
- **네트워크**: LLM API 호출 최적화
- **캐싱**: 적절한 캐시 전략 적용

## 10. 완료 기준

### 10.1 기능적 요구사항
- ✅ 프로필 생성/수정/조회/삭제
- ✅ 개인화된 면접 질문 5개 생성
- ✅ 구체적인 학습 경로 추천
- ✅ API 문서화 완료

### 10.2 기술적 요구사항
- ✅ 테스트 커버리지 80% 이상
- ✅ API 응답시간 3초 이내
- ✅ 에러 핸들링 완료
- ✅ 로깅 시스템 구축

### 10.3 운영 준비
- ✅ Docker 환경 구성
- ✅ Health Check 엔드포인트
- ✅ 환경별 설정 분리
- ✅ 배포 가이드 문서

이 가이드를 따라 단계별로 구현하면 **안정적이고 확장 가능한 MVP**를 완성할 수 있습니다.