# 새 AI 기능 추가 가이드

> 관련 문서: [AI 감정 분석 연동 가이드](./ai-integration.md)

이 문서는 감정 분석 외에 **새로운 AI 기능을 추가할 때** 따라야 할 절차를 설명합니다.

---

## 핵심 원칙

이 프로젝트는 **LLM Gateway Pattern**을 사용합니다.

OpenAI 어댑터·resilience4j 설정은 `DefaultAiGateway` 안에 구성되어 있습니다.  
새 기능을 추가할 때 **건드리지 않아도 되는 것**:

- `OpenAiAiProvider` — 변경 없음
- `AiConfig` 프로바이더 빈 — 변경 없음
- `application.yml` resilience4j 설정 — 변경 없음

**추가하는 것만** 작성합니다:

| 추가 항목 | 위치 |
|---|---|
| 프롬프트 파일 | `resources/prompts/기능명-v1.st` |
| 결과 타입 | `ai/domain/` 또는 기능 도메인 |
| 응답 타입 | `기능명/application/` |
| application 서비스 | `기능명/application/` |
| 프롬프트 빈 등록 | `AiConfig.java` |

---

## 호출 흐름

```
XxxService.xxx()
    ↓ aiGateway.call(prompt)
DefaultAiGateway
    ├── OpenAiAiProvider.call()    ← 성공 시 바로 반환
    └── StaticAiProvider.call()    ← 실패 시 JSON 기본값 반환
    ↓ AiGatewayResult(rawResponse, providerName)
XxxService → ObjectMapper로 파싱 → XxxResponse 반환
```

기능별로 달라지는 것은 **프롬프트 내용**과 **파싱 로직**뿐입니다.

---

## Step-by-Step: 댓글 요약 추가 예시

### Step 1. 프롬프트 파일 추가

파일: `src/main/resources/prompts/comment-summary-v1.st`

```text
당신은 커뮤니티 게시글 댓글 요약 전문가입니다.
반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 포함하지 마세요.

{"summary":"100자 이내 요약","tone":"CALM|NEUTRAL|URGENT"}

댓글 내용:
{content}
```

규칙:
- `{content}` 플레이스홀더는 그대로 둡니다. 서비스에서 치환됩니다.
- 응답 형식은 JSON으로 강제하고, 허용 가능한 값 범위를 명시합니다.
- 파일명은 `기능명-버전.st` 형식을 따릅니다.

---

### Step 2. 결과 타입 정의

AI 응답을 파싱할 내부 타입과, 서비스 외부로 노출할 응답 타입을 분리합니다.

**내부 파싱 타입** — `src/main/java/com/ddd/webbb/ai/domain/CommentSummaryResult.java`

```java
package com.ddd.webbb.ai.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CommentSummaryResult(
        @JsonProperty("summary") String summary,
        @JsonProperty("tone") String tone) {

    public boolean isValid() {
        return summary != null && !summary.isBlank()
                && tone != null && !tone.isBlank();
    }
}
```

**외부 응답 타입** — `src/main/java/com/ddd/webbb/comment/application/CommentSummaryResponse.java`

```java
package com.ddd.webbb.comment.application;

public record CommentSummaryResponse(
        String summary,
        String tone,
        String usedProvider) {}
```

---

### Step 3. AiConfig에 프롬프트 빈 등록

`AiConfig.java`에 프롬프트 빈만 추가합니다. 프로바이더 빈은 추가하지 않습니다.

```java
@Bean
@Qualifier("commentSummaryPromptTemplate")
public String commentSummaryPromptTemplate(AiProperties properties) throws IOException {
    ClassPathResource resource =
            new ClassPathResource("prompts/comment-summary-" + properties.promptVersion() + ".st");
    return resource.getContentAsString(StandardCharsets.UTF_8);
}
```

---

### Step 4. Application 서비스 추가

`src/main/java/com/ddd/webbb/comment/application/CommentSummaryService.java`

```java
package com.ddd.webbb.comment.application;

import com.ddd.webbb.ai.domain.AiGateway;
import com.ddd.webbb.ai.domain.AiGatewayResult;
import com.ddd.webbb.ai.domain.CommentSummaryResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class CommentSummaryService {

    private static final Logger log = LoggerFactory.getLogger(CommentSummaryService.class);

    private final AiGateway aiGateway;
    private final ObjectMapper objectMapper;
    private final String promptTemplate;

    public CommentSummaryService(
            AiGateway aiGateway,
            ObjectMapper objectMapper,
            @Qualifier("commentSummaryPromptTemplate") String promptTemplate) {
        this.aiGateway = aiGateway;
        this.objectMapper = objectMapper;
        this.promptTemplate = promptTemplate;
    }

    public CommentSummaryResponse summarize(String commentText) {
        String prompt = promptTemplate.replace("{content}", commentText);
        AiGatewayResult gatewayResult = aiGateway.call(prompt);
        CommentSummaryResult result = parseResponse(gatewayResult.rawResponse());
        return new CommentSummaryResponse(result.summary(), result.tone(), gatewayResult.providerName());
    }

    private CommentSummaryResult parseResponse(String json) {
        try {
            CommentSummaryResult result = objectMapper.readValue(json.trim(), CommentSummaryResult.class);
            if (result.isValid()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("댓글 요약 파싱 실패, 기본값 사용: {}", json);
        }
        return new CommentSummaryResult("요약 실패", "NEUTRAL");
    }
}
```

---

### 완성된 파일 구조

```
추가된 파일
├── resources/prompts/comment-summary-v1.st          ← 프롬프트
├── ai/domain/CommentSummaryResult.java              ← 파싱 대상 타입
├── comment/application/CommentSummaryResponse.java  ← 외부 응답 타입
├── comment/application/CommentSummaryService.java   ← 진입점
└── ai/infrastructure/config/AiConfig.java           ← 프롬프트 빈만 추가

건드리지 않은 파일
├── ai/infrastructure/gateway/OpenAiAiProvider.java  ← 그대로
├── ai/infrastructure/gateway/DefaultAiGateway.java  ← 그대로
└── application.yml (resilience4j 설정)              ← 그대로
```

---

## 테스트 작성 패턴

`AiGateway`를 목으로 주입해 테스트합니다. 실제 OpenAI 호출은 필요 없습니다.

```java
class CommentSummaryServiceTest {

    private AiGateway aiGateway;
    private CommentSummaryService service;

    @BeforeEach
    void setUp() {
        aiGateway = mock(AiGateway.class);
        service = new CommentSummaryService(aiGateway, new ObjectMapper(), "댓글: {content}");
    }

    @Test
    void 정상_응답을_파싱하여_반환한다() {
        String json = "{\"summary\":\"잘 요약됨\",\"tone\":\"CALM\"}";
        given(aiGateway.call(anyString())).willReturn(new AiGatewayResult(json, "OPENAI"));

        CommentSummaryResponse response = service.summarize("긴 댓글 내용...");

        assertThat(response.summary()).isEqualTo("잘 요약됨");
        assertThat(response.usedProvider()).isEqualTo("OPENAI");
    }

    @Test
    void 파싱_실패시_기본값을_반환한다() {
        given(aiGateway.call(anyString())).willReturn(new AiGatewayResult("잘못된 JSON", "OPENAI"));

        CommentSummaryResponse response = service.summarize("댓글");

        assertThat(response.summary()).isEqualTo("요약 실패");
    }

    @Test
    void 프롬프트에_입력_텍스트가_포함된다() {
        given(aiGateway.call(contains("실제 댓글"))).willReturn(
                new AiGatewayResult("{\"summary\":\"요약\",\"tone\":\"CALM\"}", "OPENAI"));

        service.summarize("실제 댓글");

        verify(aiGateway).call(contains("실제 댓글"));
    }
}
```

참고할 기존 테스트:

- `src/test/java/com/ddd/webbb/ai/application/AiAnalysisServiceTest.java` — AiGateway 목 패턴
- `src/test/java/com/ddd/webbb/ai/infrastructure/gateway/DefaultAiGatewayTest.java` — 폴백 체인 검증

---

## 체크리스트

- [ ] `resources/prompts/기능명-v1.st` 추가 (`{content}` 플레이스홀더 포함)
- [ ] 결과 타입 (`XxxResult`) 정의 + `@JsonProperty` + `isValid()` 구현
- [ ] 응답 타입 (`XxxResponse`) 정의
- [ ] `AiConfig`에 프롬프트 빈 등록 (`@Qualifier("xxxPromptTemplate")`)
- [ ] application 서비스 작성 (`AiGateway` 주입 → 프롬프트 빌드 → gateway.call → 파싱)
- [ ] 테스트 작성 (정상 파싱, 파싱 실패 기본값, 프롬프트 치환 확인)
- [ ] Swagger 또는 docs 문서화
