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

기본 프로필은 `local`입니다. 처음 실행할 때는 로컬 설정 파일을 템플릿에서 복사해 만듭니다.

```bash
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

필요하면 `src/main/resources/application-local.yml`에서 로컬 DB, Redis, JWT 값을 수정한 뒤 실행합니다.

> **Flyway 마이그레이션:** 앱 실행 시 Flyway가 자동으로 DB 스키마를 초기화합니다.
> 엔티티를 변경할 때는 반드시 마이그레이션 파일을 함께 추가해야 합니다.
> 자세한 내용은 [docs/flyway/README.md](docs/flyway/README.md)를 참고하세요.

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
> AI 감정 분석 기능 연동 방법은 [docs/ai-integration.md](docs/ai-integration.md)를 참고하세요.
> DB 스키마 버전 관리 방법은 [docs/flyway/README.md](docs/flyway/README.md)를 참고하세요.

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

---

## 커밋 메시지 규칙

이 저장소는 GitHub Actions에서 `release-please`를 사용해 릴리즈와 배포를 자동화합니다.
`main` 브랜치에 반영되는 커밋 메시지는 반드시 Conventional Commit 형식을 따라야 합니다.

### 기본 형식

```text
type: 제목
```

scope가 필요하면 아래 형식을 사용합니다.

```text
type(scope): 제목
```

브레이킹 체인지가 있으면 `!`를 붙입니다.

```text
type!: 제목
type(scope)!: 제목
```

### 사용 가능한 type 예시

- `feat`: 새로운 기능
- `fix`: 버그 수정
- `refactor`: 리팩토링
- `docs`: 문서 수정
- `test`: 테스트 추가/수정
- `chore`: 빌드, 설정, 포맷 등 기타 작업

### 권장 예시

```text
feat: AI 공통 감정 분석 서비스 추가
fix(auth): JWT 예외 처리 수정
chore: AI 코드 포맷 정리
docs(cicd): 배포 흐름 문서화
test(ai): 감정 분석 서비스 테스트 보강
```

### 주의사항

- 커밋 제목 앞에 `[BE]` 같은 접두사는 붙이지 않습니다.
- `로깅 설정 추가`, `OpenAPI 설정 보강`처럼 `type:` 없이 시작하면 안 됩니다.
- PR 번호는 붙어도 되지만, 제목 시작은 반드시 `feat:`, `fix:` 같은 Conventional Commit 형식이어야 합니다.
- 자동 배포와 직접 연결되는 변경은 가능하면 `feat:` 또는 `fix:`를 우선 사용합니다.
