# WEBBB BE

## 로컬 개발 환경 설정

### 사전 요구사항

- Java 21+
- Docker & Docker Compose

### 1. 인프라 실행

```bash
docker-compose up -d
```

| 서비스 | 포트 | 계정 |
|--------|------|------|
| MySQL  | 3306 | webbb / webbb |
| Redis  | 6379 | - |

DB명: `webbb`

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

또는 IntelliJ에서 `WebbbApplication.java` 실행

### 3. Swagger UI 접속

애플리케이션 실행 후 브라우저에서 접속합니다.

| 항목 | URL |
|------|-----|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

> JWT 인증이 필요한 API는 Swagger UI 우측 상단 **Authorize** 버튼에서 `Bearer <토큰>` 형식으로 입력하세요.

### 4. 빌드

```bash
./gradlew build
```

### 인프라 종료

```bash
docker-compose down
```

데이터까지 초기화하려면:

```bash
docker-compose down -v
```

---

## 패키지 구조

> 레이어 의존 방향 및 각 패키지별 파일 목록은 [docs/architecture.md](docs/architecture.md)를 참고하세요.

DDD 스타일 레이어드 아키텍처를 적용합니다. 도메인(Feature) 기반으로 모듈을 분리하고, 각 모듈 내부에 `domain / application / infrastructure / interfaces` 4-레이어를 적용합니다.

```
com.dnd.webbb/
├── global/                          # 전역 공통 설정
│   ├── config/                      # JPA, Security, Swagger 설정
│   ├── common/
│   │   ├── response/                # 공통 응답 래퍼 (ApiResponse)
│   │   ├── exception/               # AppException, ErrorCode, GlobalExceptionHandler
│   │   └── entity/                  # BaseEntity (createdAt, updatedAt 등)
│   └── auth/                        # JWT 필터 및 유틸 (JwtProvider, JwtAuthFilter)
│
├── user/
│   ├── domain/                      # User 엔티티, UserRepository 인터페이스, enum
│   ├── application/                 # UserService, 서비스 내부 DTO
│   ├── infrastructure/              # QueryDSL 등 복잡한 쿼리 구현체 (단순하면 생략)
│   └── interfaces/
│       ├── UserController.java
│       └── dto/                     # UserRequest, UserResponse
│
├── auth/                            # Google OAuth + JWT 로그인 흐름
│   ├── domain/
│   ├── application/                 # AuthService
│   ├── infrastructure/              # GoogleOAuthClient
│   └── interfaces/
│       └── dto/
│
└── cohort/
    ├── domain/
    ├── application/
    ├── infrastructure/
    └── interfaces/
        └── dto/
```

### 핵심 설계 원칙

1. **도메인 엔티티와 인터페이스 DTO 분리** — `User.java` ≠ `UserResponse.java`
2. **Repository 인터페이스는 domain 레이어에** — 인프라 의존 방향 역전
3. **Service는 application 레이어에만** — 컨트롤러에서 직접 레포지토리 접근 금지
4. **단순한 쿼리는 infrastructure/ 생략 가능** — Spring Data JPA 인터페이스만으로 충분할 때
