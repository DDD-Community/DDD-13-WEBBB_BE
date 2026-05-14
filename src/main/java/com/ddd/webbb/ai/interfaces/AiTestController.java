package com.ddd.webbb.ai.interfaces;

import com.ddd.webbb.ai.application.AiAnalysisResponse;
import com.ddd.webbb.ai.application.AiAnalysisService;
import com.ddd.webbb.ai.domain.PostContent;
import com.ddd.webbb.ai.interfaces.dto.AiAnalyzeTestRequest;
import com.ddd.webbb.ai.interfaces.dto.AiAnalyzeTestResponse;
import com.ddd.webbb.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/test")
@Tag(name = "AI Test", description = "AI 감정 분석 테스트 API")
public class AiTestController {

    private final AiAnalysisService aiAnalysisService;

    public AiTestController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping("/analyze")
    @Operation(
            summary = "감정 분석 테스트",
            description =
                    """
            게시글 내용을 AI로 분석해 감정 유형, 몬스터 HP, 신뢰도, 분류 근거를 반환합니다.

            동작 방식:
            1. 입력 문장에서 위기 키워드(예: 자해/자살 표현)를 먼저 검사합니다.
            2. 위기 표현이 감지되면 AI 호출 없이 안전 기본값과 `crisisDetected=true`를 반환합니다.
            3. 위기 표현이 없으면 Claude -> OpenAI 순서로 프롬프트 기반 감정 분석을 시도합니다.
            4. AI 응답이 실패하거나 파싱 불가하면 STATIC fallback 결과를 반환합니다.

            현재 사용 프롬프트 요약:
            - 역할: 익명 감정 지지 커뮤니티의 감정 분석 전문가
            - 감정 분류: ANXIETY, LETHARGY, LONELINESS, SELF_DEPRECATION, IRRITATION 중 1개만 선택
            - HP 기준: 10(가벼움), 20(보통), 30(심각)
            - 지침: 언어적 단서와 복합 감정 중 가장 강한 감정을 기준으로 판단
            - 응답 형식: 반드시 JSON만 반환

            프롬프트 원문:
            ```
            당신은 익명 감정 지지 커뮤니티 "오구오구"의 AI 감정 분석 전문가입니다.
            사용자가 작성한 고민 게시글을 분석하여 감정 유형과 몬스터 HP를 결정합니다.

            감정 유형:
            - ANXIETY: 미래에 대한 걱정, 불확실함, 초조함, 떨림
            - LETHARGY: 의욕 상실, 번아웃, 아무것도 하기 싫음, 에너지 고갈
            - LONELINESS: 고립감, 이해받지 못함, 혼자라는 느낌, 소외감
            - SELF_DEPRECATION: 자기 자신에 대한 부정, 스스로 비판, 자존감 저하
            - IRRITATION: 분노, 억울함, 불만족, 화남

            HP 기준:
            - 10: 가벼운 일상적 감정
            - 20: 명확한 감정 표현, 일정 기간 지속된 어려움
            - 30: 강렬하고 심각한 감정, 오래 지속된 복합적 고통

            응답 형식:
            {"emotionType":"ANXIETY|LETHARGY|LONELINESS|SELF_DEPRECATION|IRRITATION","hp":10,"confidence":0.0,"reason":"분류근거50자이내"}
            ```
            """,
            requestBody =
                    @RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            examples =
                                                    @ExampleObject(
                                                            name = "감정 분석 요청 예시",
                                                            value =
                                                                    """
                        {
                          "content": "면접 결과를 기다리는데 계속 불안하고 심장이 떨려요."
                        }
                        """))),
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                        responseCode = "200",
                        description = "감정 분석 성공",
                        content =
                                @Content(
                                        examples =
                                                @ExampleObject(
                                                        name = "감정 분석 응답 예시",
                                                        value =
                                                                """
                            {
                              "success": true,
                              "data": {
                                "emotionType": "ANXIETY",
                                "hp": 30,
                                "confidence": 0.91,
                                "reason": "불안과 긴장 표현이 반복적으로 드러남",
                                "crisisDetected": false,
                                "usedProvider": "CLAUDE"
                              },
                              "error": null
                            }
                            """)))
            })
    public ApiResponse<AiAnalyzeTestResponse> analyze(
            @Valid @org.springframework.web.bind.annotation.RequestBody
                    AiAnalyzeTestRequest request) {
        AiAnalysisResponse response =
                aiAnalysisService.analyze(new PostContent(null, request.content()));
        return ApiResponse.ok(AiAnalyzeTestResponse.from(response));
    }
}
