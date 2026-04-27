# Retrospective AI PoC

취업 준비 과정에서 겪은 좌절감을 **KPT(Keep / Problem / Try)** 프레임워크와 응원 메시지로 구조화해 돌려주는 Spring Boot 3 + Spring AI 1.0 + Java 21 PoC.

---

## 1. 풀려는 문제

취준생은 면접·코딩테스트 직후 강한 감정적 자책에 빠지기 쉽다.

- "난 바보야", "이번 생은 망했어" 같은 자기-공격 사고
- 무엇을 잘했고 무엇을 다음에 바꿔야 하는지 **객관적 분리**가 안 됨

이 PoC는 **사용자의 파편화된 자유 텍스트**를 받아 다음 4가지로 정리해 돌려준다.

| 필드            | 의미                                         |
| --------------- | -------------------------------------------- |
| `keep`          | 사용자가 잘하고 있는 점 (무조건 발굴)        |
| `problem`       | 감정 자책을 배제한 객관적·개선 가능한 사실만 |
| `try`           | 내일 당장 실행 가능한 작은 액션 1~2개        |
| `cheer_message` | 따뜻한 위로·응원 한마디                      |

LLM의 비결정적 출력을 **도메인 모델로 안전하게 매핑**하고, 외부 의존(OpenAI) 장애 시에도 서비스가 죽지 않도록 **Resilience4j**로 감싸는 게 핵심이다.

---

## 2. 설계 의도 / 풀려고 한 기술적 문제

| 문제                                               | 해결                                                                                                      |
| -------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| LLM 호출은 수 초 단위 블로킹 IO → 톰캣 스레드 고갈 | **Java 21 Virtual Threads** 활성화 (`spring.threads.virtual.enabled=true`)                                |
| 일시적 5xx / 네트워크 실패 시 즉시 사용자 에러     | **Resilience4j `@Retry`** (max 2회)                                                                       |
| OpenAI 장애 폭풍 시 모든 요청이 타임아웃 후 실패   | **Resilience4j `@CircuitBreaker`** + **FallbackKptAnalyzer** (Clova/Mock으로 자동 전환) |
| `ChatClient.call()` 자체에 타임아웃 없음           | `JdkClientHttpRequestFactory`에 connect/read **HTTP timeout** 설정                                        |
| LLM JSON 파싱 실패도 retry되면 무의미              | 예외 분리: `RetryableUpstreamException` vs `PermanentResponseException`                                   |
| 비즈니스 예외와 HTTP 응답의 강한 결합              | **Java 21 Pattern Matching for switch**를 통한 functional response mapping (ResponseEntity)               |
| 한국어 디렉터리, 환경 격차로 빌드 깨짐             | Foojay toolchain resolver + JDK 21 toolchain                                                              |
| 응답 형태가 클라이언트마다 제각각 (`tryList` 등)   | `@JsonProperty("try")`, `@JsonProperty("cheer_message")`로 외부 표현 고정                                 |
| 도메인 객체에 LLM 응답이 그대로 노출               | 도메인 `KptAnalysis` ↔ LLM `KptResponse` ↔ 외부 `RetrospectiveResponse` **3계층 분리**                    |
| 의존 방향 위반 (application → interfaces)          | **Hexagonal**: `application/port/KptAnalyzer` 인터페이스 + `infrastructure/ai/SpringAiKptAnalyzer` 어댑터 |

---

## 3. 아키텍처

```
com.dnd.poc.retrospective
├── RetrospectiveApplication          Spring Boot entrypoint
├── config/
│   ├── AiConfig                      ChatClient bean + RestClient HTTP timeout + 시스템 프롬프트 빈
│   └── AiProperties                  @ConfigurationProperties (record + Bean Validation)
├── domain/                           순수 도메인 (외부 의존 0)
│   ├── KptAnalysis                   VO (record + 자가검증 + List.copyOf 불변)
│   ├── RetrospectiveContext          입력 VO + MAX_LENGTH 단일 출처
│   ├── RetrospectiveResult           sealed: Success | Failure + ErrorCode enum
│   └── exception/
│       ├── RetrospectiveGenerationException   도메인 기본 예외 (ErrorCode 보유)
│       ├── RetryableUpstreamException         재시도 대상 (UPSTREAM_*)
│       └── PermanentResponseException         재시도 금지 (INVALID/EMPTY)
├── application/
│   ├── port/KptAnalyzer              outbound port (인터페이스)
│   └── RetrospectiveService          예외 → Result 변환만 담당하는 얇은 application service
├── infrastructure/ai/
│   ├── SpringAiKptAnalyzer           @Retry + @CircuitBreaker, 예외 분류 로직
│   └── KptResponse                   LLM 응답 매핑 record
└── interfaces/
    ├── RetrospectiveController       sealed switch로 Success/Failure 분기
    ├── CorrelationIdFilter           X-Correlation-Id MDC 주입
    ├── dto/{RetrospectiveRequest, RetrospectiveResponse}
    └── exception/GlobalExceptionHandler   ProblemDetail (RFC 7807) 변환
```

**스타일**: Hexagonal Architecture (Ports & Adapters) + DDD building blocks (VO, Application Service, Domain Exception).

**의존 방향**:
```
interfaces ──▶ application (port 정의) ◀── infrastructure (adapter 구현)
      │               │                          │
      └───────▶ domain (model/result) ◀──────────┘
```

**핵심 패턴**:
- **Decorator/Composite**: `FallbackKptAnalyzer`가 주 모델 실패 시 예비 모델로 자동 전환.
- **Functional Mapping**: Controller에서 `switch (result)` 패턴 매칭을 통해 성공/실패 응답을 분기.

---

## 4. 기술 스택

| 영역       | 사용 기술                                                                        |
| ---------- | -------------------------------------------------------------------------------- |
| 언어       | Java 21 (record, sealed interface, pattern matching for switch, virtual threads) |
| 프레임워크 | Spring Boot 3.4.5, Spring AI 1.0.0 (OpenAI starter)                              |
| 회복탄력성 | Resilience4j 2.2 (`@Retry`, `@CircuitBreaker`)                                   |
| 검증       | Jakarta Bean Validation                                                          |
| 문서       | Springdoc OpenAPI 2.8 (Swagger UI)                                               |
| 관측       | Spring Boot Actuator + MDC correlation id                                        |
| 빌드       | Gradle 8.14 + Foojay Toolchain Resolver                                          |

---

## 5. 실행 방법

### 5.1 사전 준비

- macOS / Linux
- OpenAI API key (`sk-...`)
- JDK 21 — 미설치 시 brew로:
  ```bash
  brew install openjdk@21
  ```

### 5.2 실행

```bash
cd "<repo>/poc/juneseok"

# 옵션 A) Gradle bootRun (권장)
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
OPENAI_API_KEY=sk-... \
./gradlew --no-daemon bootRun

# 옵션 B) 빌드된 jar 실행
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
./gradlew --no-daemon build
OPENAI_API_KEY=sk-... \
$JAVA_HOME/bin/java -jar build/libs/retrospective-poc-0.0.1-SNAPSHOT.jar
```

> **주의**: 레포 루트(`DDD-13-WEBBB_BE`)에서 `./gradlew bootRun`을 돌리면 메인 프로젝트(MySQL 의존)가 떠서 `Access denied for user 'webbb'` 에러가 납니다. **반드시 `poc/juneseok/`에서 실행**하세요.

성공 시 콘솔:

```
Started RetrospectiveApplication in 2.1s on port 8082
```

---

## 6. API 사용법

### 6.1 Swagger UI (가장 쉬움)

브라우저 → http://localhost:8082/swagger-ui/index.html
→ `Retrospective AI` → `POST /api/v1/retrospective/analyze` → **Try it out**

### 6.2 curl

```bash
# 정상 호출
curl -X POST http://localhost:8082/api/v1/retrospective/analyze \
  -H 'Content-Type: application/json' \
  -d '{"context":"오늘 코테에서 시간이 부족해서 마지막 문제를 못 풀었어요. 자료구조 공부가 부족했어요."}'

# correlation id 지정 (응답 헤더 X-Correlation-Id로 echo)
curl -i -X POST http://localhost:8082/api/v1/retrospective/analyze \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-Id: trace-001' \
  -d '{"context":"면접에서 너무 긴장했어요."}'

# 검증 실패 → 400 ProblemDetail
curl -i -X POST http://localhost:8082/api/v1/retrospective/analyze \
  -H 'Content-Type: application/json' \
  -d '{"context":""}'
```

### 6.3 정상 응답

```json
{
  "keep": ["끝까지 풀려고 시도한 점", "본인 약점을 객관화한 점"],
  "problem": ["시간 분배 전략 부재", "자료구조 학습량 부족"],
  "try": ["내일 30분 자료구조 1챕터 복습", "1문제 25분 룰 적용"],
  "cheer_message": "오늘도 시도한 것만으로 충분해요. 한 걸음씩 가요."
}
```

### 6.4 에러 응답 (RFC 7807 ProblemDetail)

```json
{
  "type": "urn:problem:retrospective",
  "title": "Retrospective generation failed",
  "status": 502,
  "detail": "AI 회고 생성에 실패했습니다.",
  "errorCode": "UPSTREAM_UNAVAILABLE"
}
```

| `errorCode`            | HTTP | 의미                      | 재시도?       |
| ---------------------- | ---- | ------------------------- | ------------- |
| `UPSTREAM_TIMEOUT`     | 504  | LLM 응답 지연             | ✅ Retry 자동 |
| `UPSTREAM_UNAVAILABLE` | 502  | LLM 호출 실패 / 회로 열림 | ✅ Retry 자동 |
| `EMPTY_RESPONSE`       | 422  | LLM이 빈 응답             | ❌            |
| `INVALID_RESPONSE`     | 422  | LLM JSON 형식 깨짐        | ❌            |

### 6.5 Postman Collection

[`postman/Retrospective-AI.postman_collection.json`](postman/Retrospective-AI.postman_collection.json) 파일을 Postman에 import하면 다음이 한 번에 들어옵니다.

- **컬렉션 변수**
  - `baseUrl` (기본 `http://localhost:8082`)
  - `correlationId` (자동 UUID)
- **요청**
  1. Health check (`GET /actuator/health`)
  2. OpenAPI spec (`GET /v3/api-docs`)
  3. KPT 분석 - 정상 (코딩테스트 회고)
  4. KPT 분석 - 정상 (면접 회고)
  5. 검증 실패 - 빈 context
  6. 검증 실패 - 길이 초과 (pre-request에서 2001자 자동 생성)
  7. 검증 실패 - context 필드 누락
- 각 요청에 **Postman Tests 스크립트** 내장 (status, 응답 필드, correlation id echo 검증)

**import 방법**:

1. Postman → `File → Import` → 위 JSON 파일 선택
2. 컬렉션 우상단 ▶︎ **Run** 누르면 7개 요청을 일괄 실행하고 통과/실패 리포트 출력

### 6.6 Health / Metrics

```bash
curl http://localhost:8082/actuator/health
curl http://localhost:8082/actuator/metrics
```

---

## 7. 설정 옵션 ([application.yml](src/main/resources/application.yml))

| 키                                                                         | 기본값                            | 설명                       |
| -------------------------------------------------------------------------- | --------------------------------- | -------------------------- |
| `server.port`                                                              | 8082                              | 서버 포트                  |
| `spring.threads.virtual.enabled`                                           | true                              | Java 21 가상 스레드 활성화 |
| `spring.ai.openai.api-key`                                                 | env `OPENAI_API_KEY`              | OpenAI 키                  |
| `spring.ai.openai.chat.options.model`                                      | `gpt-4o-mini`                     | 사용 모델                  |
| `spring.ai.openai.chat.options.temperature`                                | 0.3                               | 결정성 (낮을수록 일관)     |
| `retrospective.ai.prompt-location`                                         | `classpath:prompts/kpt-system.st` | 시스템 프롬프트 위치       |
| `retrospective.ai.timeout`                                                 | 20s                               | HTTP connect/read 타임아웃 |
| `resilience4j.retry.instances.kptAnalyzer.max-attempts`                    | 2                                 | retry 횟수                 |
| `resilience4j.circuitbreaker.instances.kptAnalyzer.failure-rate-threshold` | 60                                | 회로 차단 임계 (%)         |

입력 길이 제한 = `RetrospectiveContext.MAX_LENGTH = 2000` (코드 상수, 단일 출처).

프롬프트는 [`src/main/resources/prompts/kpt-system.st`](src/main/resources/prompts/kpt-system.st)에서 외부화되어 있어 재배포 없이 수정 가능합니다.

---
