# Career Coach API

이력서 기반 개인 맞춤형 커리어 코치 챗봇 API

## 기술 스택

- Kotlin + Spring Boot 3.x
- PostgreSQL
- Google Gemini API
- Docker & Docker Compose

## 시작하기

### 사전 요구사항

- JDK 17+
- Docker & Docker Compose
- Gradle 8.x

### 개발 환경 설정

1. 데이터베이스 시작
```bash
docker-compose up -d postgres
```

2. 애플리케이션 실행
```bash
./gradlew bootRun
```

3. API 문서 확인
- Swagger UI: http://localhost:8080/swagger-ui
- API Docs: http://localhost:8080/api-docs

### 테스트 실행

```bash
# 단위 테스트
./gradlew test

# 테스트 커버리지 확인
./gradlew test jacocoTestReport
```

## 프로젝트 구조

```
src/main/kotlin/com/careercoach/
├── config/         # 설정 클래스
├── controller/     # REST 컨트롤러
├── service/        # 비즈니스 로직
├── domain/         # JPA 엔티티
├── repository/     # 데이터 접근
├── dto/           # 요청/응답 DTO
├── external/      # 외부 API 연동
└── common/        # 공통 유틸리티
```

## API 엔드포인트

- `POST /api/v1/profiles` - 프로필 생성
- `GET /api/v1/profiles/{id}` - 프로필 조회
- `PUT /api/v1/profiles/{id}` - 프로필 수정
- `DELETE /api/v1/profiles/{id}` - 프로필 삭제
- `GET /api/v1/profiles/{id}/questions` - 면접 질문 생성
- `GET /api/v1/profiles/{id}/learning-paths` - 학습 경로 생성

## 환경 변수

- `GEMINI_API_KEY`: Google Gemini API 키

## 라이선스

Private