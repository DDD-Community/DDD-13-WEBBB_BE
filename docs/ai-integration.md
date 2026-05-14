# AI 감정 분석 연동 가이드

## 목적

이 문서는 다른 개발자가 WEBBB BE의 AI 감정 분석 기능을 연동할 때 필요한 입력, 응답, 환경변수, 동작 흐름을 빠르게 이해할 수 있도록 정리한 가이드입니다.

현재 외부에서 바로 확인 가능한 엔드포인트는 테스트용 API인 `POST /api/ai/test/analyze` 입니다.

---

## 제공 기능

게시글 본문을 입력하면 서버가 아래 값을 반환합니다.

- 대표 감정 유형
- 감정 강도에 따른 HP
- AI 분류 신뢰도
- 분류 근거
- 위기 키워드 감지 여부
- 실제 응답을 생성한 provider

감정 유형은 아래 5가지 중 하나로 반환됩니다.

- `ANXIETY`
- `LETHARGY`
- `LONELINESS`
- `SELF_DEPRECATION`
- `IRRITATION`

HP는 아래 3단계만 사용합니다.

- `10`: 가벼운 감정
- `20`: 보통 강도의 감정
- `30`: 강한 감정

---

## 엔드포인트

### 1. 감정 분석 테스트 API

`POST /api/ai/test/analyze`

Content-Type:

```http
application/json
```

요청 본문:

```json
{
  "content": "면접 결과를 기다리는데 계속 불안하고 심장이 떨려요."
}
```

성공 응답 예시:

```json
{
  "success": true,
  "data": {
    "emotionType": "ANXIETY",
    "hp": 20,
    "confidence": 0.91,
    "reason": "불안과 긴장 표현이 반복적으로 드러남",
    "crisisDetected": false,
    "usedProvider": "CLAUDE"
  },
  "error": null
}
```

응답 필드 설명:

| 필드 | 타입 | 설명 |
|---|---|---|
| `emotionType` | `String` | 대표 감정 유형 |
| `hp` | `int` | 감정 강도에 따른 HP 값 (`10`, `20`, `30`) |
| `confidence` | `double` | AI가 판단한 신뢰도 |
| `reason` | `String` | 분류 근거 |
| `crisisDetected` | `boolean` | 위기 키워드가 먼저 감지되었는지 여부 |
| `usedProvider` | `String` | 실제 응답을 반환한 주체 |

`usedProvider` 값:

- `CLAUDE`
- `OPENAI`
- `STATIC`
- `CRISIS_FILTER`

실패 응답 예시:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "BAD_REQUEST",
    "message": "잘못된 요청입니다."
  }
}
```

`content`가 비어 있으면 `400 Bad Request`를 반환합니다.

---

## 동작 방식

서버는 아래 순서로 감정 분석을 수행합니다.

### 1. 위기 키워드 선검사

입력 문장에서 아래와 같은 위기 표현을 먼저 검사합니다.

- `죽고 싶`
- `자살`
- `자해`
- `스스로 목숨`
- `삶을 끝`
- `죽어버리고 싶`

위기 표현이 감지되면:

- 외부 AI를 호출하지 않습니다.
- 안전 기본 응답을 반환합니다.
- `crisisDetected=true`
- `usedProvider=CRISIS_FILTER`

### 2. AI provider 순차 호출

위기 표현이 없으면 아래 순서로 호출합니다.

1. `Claude`
2. `OpenAI`
3. `Static fallback`

각 provider는 동일한 프롬프트 규칙에 따라 JSON 응답을 반환해야 합니다.

### 3. fallback 처리

Claude 또는 OpenAI 호출 실패, 타임아웃, 파싱 오류가 발생하면 다음 provider로 넘어갑니다.

모든 AI provider가 실패하면:

- 안전 기본값을 반환합니다.
- `usedProvider=STATIC`

---

## 프롬프트 규칙

서버는 감정 분석 시 아래 규칙을 사용합니다.

- 감정 분류는 5개 타입 중 1개만 선택
- HP는 `10`, `20`, `30` 중 1개만 선택
- 게시글의 언어적 단서, 반복 표현, 감정 강도를 반영
- 복합 감정이면 가장 강한 감정을 대표값으로 선택
- 응답은 반드시 JSON 형식

실제 프롬프트 파일:

- `src/main/resources/prompts/emotion-analysis-v1.st`

프롬프트 버전은 환경변수 `PROMPT_VERSION`으로 선택합니다.

---

## 로컬 연동 방법

### 1. 환경변수 준비

루트의 `.env.example`을 참고해 `.env`를 준비합니다.

현재 로컬 최소값:

```env
OPENAI_API_KEY=your-openai-api-key
```

기본적으로 `spring-dotenv`가 루트 `.env` 파일을 읽습니다.

### 2. 인프라 실행

```bash
docker-compose up -d
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 4. API 호출

예시:

```bash
curl -X POST http://localhost:8080/api/ai/test/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "content": "요즘 아무도 저를 이해하지 못하는 것 같아 너무 외로워요."
  }'
```

Swagger 사용 시:

- `http://localhost:8080/swagger-ui/index.html`

---

## 서버 연동 예시 코드

아래 예시는 이 프로젝트 내부에서 새로운 기능을 개발할 때 `AiAnalysisService`를 어떻게 주입받고 호출하는지에 대한 가이드입니다.

핵심 원칙:

- `interfaces` 레이어에서 직접 AI provider를 호출하지 않습니다.
- `application` 레이어에서 `AiAnalysisService`를 주입받아 사용합니다.
- 입력은 `PostContent`, 출력은 `AiAnalysisResponse`를 사용합니다.

### 1. application 레이어에서 주입받아 사용하는 기본 예시

```java
package com.ddd.webbb.post.application;

import com.ddd.webbb.ai.application.AiAnalysisResponse;
import com.ddd.webbb.ai.application.AiAnalysisService;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.post.domain.Post;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostEmotionFacade {

    private final AiAnalysisService aiAnalysisService;

    public PostEmotionFacade(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @Transactional(readOnly = true)
    public AiAnalysisResponse analyze(Post post) {
        PostContent content = new PostContent(post.getId(), post.getContent());
        return aiAnalysisService.analyze(content);
    }
}
```

### 2. 게시글 생성 흐름에서 함께 사용하는 예시

게시글 저장 직후 감정 분석이 필요하다면 아래처럼 사용할 수 있습니다.

```java
package com.ddd.webbb.post.application;

import com.ddd.webbb.ai.application.AiAnalysisResponse;
import com.ddd.webbb.ai.application.AiAnalysisService;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.post.domain.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final AiAnalysisService aiAnalysisService;

    public PostService(PostRepository postRepository, AiAnalysisService aiAnalysisService) {
        this.postRepository = postRepository;
        this.aiAnalysisService = aiAnalysisService;
    }

    @Transactional
    public void create(Post post) {
        Post savedPost = postRepository.save(post);

        AiAnalysisResponse aiResponse = aiAnalysisService.analyze(
            new PostContent(savedPost.getId(), savedPost.getContent())
        );

        String emotionType = aiResponse.emotionType();
        int hp = aiResponse.hp();
        boolean crisisDetected = aiResponse.crisisDetected();

        // TODO: emotionType / hp 기반 후속 도메인 로직 수행
        // 예: PostEmotion 저장, Monster HP 반영, 위기 응답 분기 등
    }
}
```

### 3. 후속 저장 로직 분기 예시

보통 서버에서는 `emotionType`, `hp`, `crisisDetected`를 기준으로 후속 도메인 로직을 나눕니다.

```java
AiAnalysisResponse aiResponse = aiAnalysisService.analyze(
    new PostContent(post.getId(), post.getContent())
);

if (aiResponse.crisisDetected()) {
    // 일반 감정 저장 대신 위기 대응 플로우로 분기
    // 예: 별도 알림 이벤트 발행, 특수 응답 데이터 저장
    return;
}

if (aiResponse.hp() >= 30) {
    // 강한 감정 상태 처리
}

switch (aiResponse.emotionType()) {
    case "ANXIETY" -> {
        // 불안 관련 후속 처리
    }
    case "LONELINESS" -> {
        // 외로움 관련 후속 처리
    }
    default -> {
        // 기본 처리
    }
}
```

### 4. 어떤 레이어에서 호출해야 하는가

권장:

- `application` 서비스에서 `AiAnalysisService` 주입
- 도메인 저장 직후 또는 조회 조합 로직에서 호출

비권장:

- `controller`에서 직접 provider별 구현체 호출
- `domain` 엔티티 내부에서 AI 서비스 직접 호출
- `infrastructure` 구현체를 다른 도메인 서비스가 직접 의존

### 5. 내부 후속 처리 권장 방식

- `emotionType`: 감정 엔티티 저장, 추천 로직, 통계 분류 기준으로 사용
- `hp`: 감정 강도 수치 또는 몬스터 HP 계산 기준으로 사용
- `crisisDetected`: 일반 저장 플로우 대신 별도 위기 대응 플로우로 우회
- `usedProvider`: 운영 로그, 디버깅, 품질 점검용으로 사용

---

## 운영 연동 시 주의사항

- `prod` 프로필에서는 Swagger가 비활성화되어 있습니다.
- 테스트 API 엔드포인트 자체는 prod 에서도 등록됩니다.
- 운영 환경에서는 아래 환경변수 구성이 필요합니다.

| 변수 | 설명 |
|---|---|
| `DB_URL` | 운영 DB JDBC URL |
| `DB_USERNAME` | 운영 DB 계정 |
| `DB_PASSWORD` | 운영 DB 비밀번호 |
| `REDIS_HOST` | 운영 Redis 호스트 |
| `REDIS_PORT` | 운영 Redis 포트 |
| `OPENAI_API_KEY` | OpenAI API 키 |
| `CLAUDE_API_KEY` | Claude API 키 |
| `PROMPT_VERSION` | 사용할 프롬프트 버전 |
| `AI_TIMEOUT` | AI 호출 타임아웃 |

로깅 경로는 필요 시 `LOG_PATH`로 덮어쓸 수 있습니다.

---

## 연동 체크리스트

- `content`에 실제 게시글 본문이 들어가는지 확인
- 빈 문자열 요청이 들어가지 않도록 클라이언트에서 선검증
- `emotionType`, `hp`, `usedProvider`, `crisisDetected`를 기준으로 후속 로직 분기
- 위기 감지 응답(`crisisDetected=true`)에 대한 UX 또는 운영 대응 방안 정의
- 운영 환경에서는 AI API 키 누락 여부 확인

---

## 관련 파일

- `src/main/java/com/ddd/webbb/ai/application/AiAnalysisService.java`
- `src/main/java/com/ddd/webbb/ai/interfaces/AiTestController.java`
- `src/main/resources/prompts/emotion-analysis-v1.st`
- `src/main/resources/application.yml`
- `src/main/resources/application-prod.yml`

---

## 새 AI 서비스 추가 가이드

이 섹션은 감정 분석 외에 새로운 AI 기능을 서버에 추가할 때의 기준입니다.

예를 들어 아래 같은 기능이 대상입니다.

- 댓글 요약
- 게시글 답변 추천
- 공감 댓글 초안 생성
- 감정 분석 고도화 버전

핵심 원칙:

- 프롬프트 파일만 추가해서 끝내지 않습니다.
- 기능별 입력 타입, 결과 타입, provider 인터페이스, application 서비스까지 함께 설계합니다.
- controller가 provider를 직접 호출하지 않고, `application` 레이어의 공통 서비스를 통해 사용합니다.

### 1. 프롬프트 파일 추가

`src/main/resources/prompts/` 아래에 기능별 프롬프트 파일을 추가합니다.

예시:

- `emotion-analysis-v1.st`
- `comment-summary-v1.st`
- `reply-recommendation-v1.st`

권장 규칙:

- 파일명에 기능명과 버전을 함께 둡니다.
- 응답 형식은 가능하면 JSON으로 강제합니다.
- 프롬프트에 허용 가능한 값 범위를 명시합니다.

예시:

```text
당신은 커뮤니티 게시글 요약 전문가입니다.
반드시 아래 JSON 형식으로만 응답하세요.

{"summary":"100자 이내 요약","tone":"CALM|NEUTRAL|URGENT"}

게시글 내용:
{content}
```

### 2. 입력 타입 정의

기능이 받을 입력 타입을 `domain` 또는 `application` 기준으로 명확히 정의합니다.

감정 분석은 현재 아래 타입을 사용합니다.

```java
public record PostContent(Long postId, String text) {
}
```

새 기능 예시:

```java
public record CommentSummaryContent(Long commentId, String text) {
}
```

권장 규칙:

- provider에 넘길 최소 정보만 담습니다.
- 엔티티 자체를 provider로 넘기지 않습니다.

### 3. 결과 타입 정의

AI provider가 반환할 구조를 먼저 정합니다.

감정 분석은 현재 provider 내부 결과 타입과 서비스 응답 타입을 분리해서 사용합니다.

- 내부 결과: `EmotionAnalysisResult`
- 서비스 응답: `AiAnalysisResponse`

새 기능 예시:

```java
public record CommentSummaryResult(
    String summary,
    String tone
) {
    public boolean isValid() {
        return summary != null && !summary.isBlank()
            && tone != null && !tone.isBlank();
    }
}
```

서비스 바깥으로 넘길 최종 응답도 별도로 정의할 수 있습니다.

```java
public record CommentSummaryResponse(
    String summary,
    String tone,
    String usedProvider
) {
}
```

### 4. 기능별 provider 인터페이스 추가

현재 감정 분석은 `EmotionAnalyzer` 인터페이스를 두고 Claude/OpenAI/Static 구현체가 따릅니다.

새 기능도 같은 방식으로 기능별 인터페이스를 둡니다.

예시:

```java
public interface CommentSummarizer {
    CommentSummaryResult summarize(CommentSummaryContent content);

    String providerName();
}
```

권장 이유:

- provider 구현 교체가 쉬움
- fallback 구조를 공통 서비스에서 다루기 쉬움
- 테스트가 단순해짐

### 5. provider 구현 추가

구현체는 `src/main/java/com/ddd/webbb/ai/infrastructure/adapter/` 아래에 추가합니다.

예시:

- `ClaudeCommentSummarizer`
- `OpenAiCommentSummarizer`
- `StaticCommentSummaryFallback`

구현체 역할:

- 프롬프트 템플릿 주입
- `{content}` 치환
- 모델 호출
- JSON 파싱
- 유효성 검증
- 예외를 `RetryableAiException` 또는 `PermanentAiException`으로 변환

기본 패턴 예시:

```java
public class OpenAiCommentSummarizer implements CommentSummarizer {

    private final ChatClient chatClient;
    private final String promptTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCommentSummarizer(ChatClient chatClient, String promptTemplate) {
        this.chatClient = chatClient;
        this.promptTemplate = promptTemplate;
    }

    @Override
    public CommentSummaryResult summarize(CommentSummaryContent content) {
        String prompt = promptTemplate.replace("{content}", content.text());
        String response = chatClient.prompt().user(prompt).call().content();

        try {
            CommentSummaryResult result = objectMapper.readValue(response, CommentSummaryResult.class);
            if (!result.isValid()) {
                throw new PermanentAiException(AiErrorCode.INVALID_RESPONSE, "유효하지 않은 AI 응답");
            }
            return result;
        } catch (PermanentAiException e) {
            throw e;
        } catch (Exception e) {
            throw new RetryableAiException(AiErrorCode.SERVICE_UNAVAILABLE, "AI 호출 실패", e);
        }
    }

    @Override
    public String providerName() {
        return "OPENAI";
    }
}
```

### 6. application 서비스 추가

실제 다른 도메인에서 호출할 진입점은 `application` 레이어에 둡니다.

감정 분석은 현재 `AiAnalysisService`가 이 역할을 합니다.

새 기능도 같은 방식으로 오케스트레이션 서비스를 둡니다.

예시:

```java
@Service
public class CommentSummaryService {

    private final List<CommentSummarizer> summarizers;

    public CommentSummaryService(List<CommentSummarizer> summarizers) {
        this.summarizers = summarizers;
    }

    public CommentSummaryResponse summarize(CommentSummaryContent content) {
        for (CommentSummarizer summarizer : summarizers) {
            try {
                CommentSummaryResult result = summarizer.summarize(content);
                if (result.isValid()) {
                    return new CommentSummaryResponse(
                        result.summary(),
                        result.tone(),
                        summarizer.providerName()
                    );
                }
            } catch (Exception ignored) {
            }
        }

        return new CommentSummaryResponse("요약 실패", "NEUTRAL", "STATIC");
    }
}
```

이 서비스에서 처리할 것:

- provider 호출 순서
- 사전 검증
- 위기/정책 필터
- fallback
- 최종 응답 변환

### 7. 설정과 빈 등록 추가

새 기능을 쓰려면 프롬프트와 provider 구현체를 Spring Bean으로 등록해야 합니다.

현재 감정 분석은 `AiConfig`가 아래 역할을 합니다.

- 프롬프트 파일 로드
- Claude/OpenAI adapter 생성
- `AiProperties` 주입

새 기능도 같은 방식으로 추가합니다.

예시:

```java
@Bean
public OpenAiCommentSummarizer openAiCommentSummarizer(
        OpenAiChatModel model,
        AiProperties properties
) throws IOException {
    String template = loadPromptTemplate("comment-summary-" + properties.promptVersion());
    return new OpenAiCommentSummarizer(ChatClient.builder(model).build(), template);
}
```

기능이 많아지면 `AiConfig`를 기능별로 분리하는 것을 권장합니다.

예시:

- `EmotionAiConfig`
- `SummaryAiConfig`
- `RecommendationAiConfig`

### 8. 환경변수와 설정 분리

현재 공통 설정:

- `PROMPT_VERSION`
- `AI_TIMEOUT`
- `OPENAI_API_KEY`
- `CLAUDE_API_KEY`

기능이 늘어나면 설정도 기능별로 나누는 편이 좋습니다.

예시:

```yaml
app:
  ai:
    emotion:
      prompt-version: v1
    summary:
      prompt-version: v1
      timeout: 10s
```

이렇게 분리하면 기능별 프롬프트 실험과 롤백이 쉬워집니다.

### 9. 테스트 추가

새 AI 기능을 추가할 때는 최소 아래 테스트를 같이 만듭니다.

- application 서비스 테스트
- provider 응답 파싱 테스트
- fallback 동작 테스트
- controller/API 테스트가 있다면 요청/응답 테스트

감정 분석 기준 현재 참고할 테스트:

- `src/test/java/com/ddd/webbb/ai/application/AiAnalysisServiceTest.java`
- `src/test/java/com/ddd/webbb/ai/infrastructure/ClaudeEmotionAnalyzerTest.java`
- `src/test/java/com/ddd/webbb/ai/infrastructure/OpenAiEmotionAnalyzerTest.java`
- `src/test/java/com/ddd/webbb/ai/interfaces/AiTestControllerTest.java`

### 10. 최종 체크리스트

새 AI 기능을 추가할 때 아래 항목이 모두 있는지 확인합니다.

- 프롬프트 파일 추가
- 입력 타입 정의
- 결과 타입 정의
- provider 인터페이스 정의
- Claude/OpenAI/fallback 구현 추가
- application 서비스 추가
- Spring Bean 등록
- 환경변수/설정 반영
- 테스트 추가
- Swagger 또는 docs 문서화
