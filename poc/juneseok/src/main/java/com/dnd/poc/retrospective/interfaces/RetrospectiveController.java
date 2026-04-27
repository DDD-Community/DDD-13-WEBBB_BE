package com.dnd.poc.retrospective.interfaces;

import com.dnd.poc.retrospective.application.RetrospectiveService;
import com.dnd.poc.retrospective.domain.RetrospectiveContext;
import com.dnd.poc.retrospective.domain.RetrospectiveResult;
import com.dnd.poc.retrospective.domain.RetrospectiveResult.Failure;
import com.dnd.poc.retrospective.domain.RetrospectiveResult.Success;
import com.dnd.poc.retrospective.domain.exception.RetrospectiveGenerationException;
import com.dnd.poc.retrospective.interfaces.dto.RetrospectiveRequest;
import com.dnd.poc.retrospective.interfaces.dto.RetrospectiveResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Retrospective AI", description = "AI KPT 회고 도우미 API")
@RestController
@RequestMapping("/api/v1/retrospective")
public class RetrospectiveController {

    private final RetrospectiveService service;

    public RetrospectiveController(RetrospectiveService service) {
        this.service = service;
    }

    @Operation(
            summary = "KPT 회고 분석",
            description = "취준생의 텍스트를 KPT(Keep/Problem/Try) 프레임워크와 응원 메시지로 구조화해 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "분석 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RetrospectiveResponse.class))),
            @ApiResponse(responseCode = "400", description = "입력 검증 실패 (빈 값 / 2000자 초과)",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "AI 응답이 빈/잘못된 형식 (EMPTY_RESPONSE, INVALID_RESPONSE)",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "업스트림 LLM 호출 실패 (UPSTREAM_UNAVAILABLE)",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "504", description = "업스트림 LLM 타임아웃 (UPSTREAM_TIMEOUT)",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(value = "/analyze",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> analyze(@Valid @RequestBody RetrospectiveRequest request) {
        RetrospectiveResult result = service.analyze(new RetrospectiveContext(request.context()));

        return switch (result) {
            case Success s -> ResponseEntity.ok(RetrospectiveResponse.from(s.analysis()));
            case Failure f -> ResponseEntity
                    .status(f.code().getHttpStatus())
                    .body(ProblemDetail.forStatusAndDetail(
                            org.springframework.http.HttpStatusCode.valueOf(f.code().getHttpStatus()),
                            f.detail()));
        };
    }
}
