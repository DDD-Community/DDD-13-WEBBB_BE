package com.dnd.poc.sentiment.interfaces;

import com.dnd.poc.global.common.response.ApiResponse;
import com.dnd.poc.sentiment.application.SentimentService;
import com.dnd.poc.sentiment.interfaces.dto.SentimentAnalyzeSuccessResponse;
import com.dnd.poc.sentiment.interfaces.dto.SentimentErrorResponse;
import com.dnd.poc.sentiment.interfaces.dto.SentimentRequest;
import com.dnd.poc.sentiment.interfaces.dto.SentimentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sentiment", description = "감성 분석 API")
@RestController
@RequestMapping("/api/sentiment")
public class SentimentController {

    private final SentimentService sentimentService;

    public SentimentController(SentimentService sentimentService) {
        this.sentimentService = sentimentService;
    }

    @Operation(summary = "텍스트 감성 분석", description = "입력한 한국어 문장을 분석해 감성 라벨과 점수, 근거 키워드를 반환합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "감성 분석 요청 본문",
            content =
                    @Content(
                            schema = @Schema(implementation = SentimentRequest.class),
                            examples =
                                    @ExampleObject(
                                            name = "감성 분석 요청 예시",
                                            value =
                                                    """
                    {
                      "text": "오늘 발표가 잘 끝나서 정말 기분이 좋아요."
                    }
                    """)))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "분석 성공",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        SentimentAnalyzeSuccessResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "성공 응답 예시",
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "감성 분석이 완료되었습니다.",
                          "data": {
                            "label": "POSITIVE",
                            "score": 0.87,
                            "confidence": 0.93,
                            "keywords": ["기분", "좋다"],
                            "reason": "긍정 감성 표현이 명확하게 포함됨"
                          },
                          "timestamp": "2026-04-22T10:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "유효성 검증 실패",
                content =
                        @Content(
                                schema = @Schema(implementation = SentimentErrorResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "검증 실패 응답 예시",
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "유효하지 않은 요청입니다.",
                          "errors": [
                            { "field": "text", "reason": "분석할 텍스트를 입력해주세요." }
                          ],
                          "timestamp": "2026-04-22T10:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "502",
                description = "CLOVA API 오류",
                content =
                        @Content(
                                schema = @Schema(implementation = SentimentErrorResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "외부 API 오류 응답 예시",
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "CLOVA API 호출에 실패했습니다.",
                          "timestamp": "2026-04-22T10:00:00"
                        }
                        """)))
    })
    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<SentimentResponse>> analyze(
            @RequestBody @Valid SentimentRequest request) {
        SentimentResponse response =
                SentimentResponse.from(sentimentService.analyze(request.text()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("감성 분석이 완료되었습니다.", response));
    }
}
