package com.ddd.webbb.post.interfaces;

import com.ddd.webbb.global.common.response.ApiResponse;
import com.ddd.webbb.global.security.CustomUserPrincipal;
import com.ddd.webbb.post.application.PostService;
import com.ddd.webbb.post.domain.PostOrder;
import com.ddd.webbb.post.domain.PostSearchCondition;
import com.ddd.webbb.post.interfaces.dto.PostCreateRequest;
import com.ddd.webbb.post.interfaces.dto.PostCreateResponse;
import com.ddd.webbb.post.interfaces.dto.PostDetailResponse;
import com.ddd.webbb.post.interfaces.dto.PostListResponse;
import com.ddd.webbb.post.interfaces.dto.PostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Post", description = "게시글 API")
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @Operation(
            summary = "게시글 작성 (글 작성)",
            description =
                    "인증된 사용자가 고민글을 작성합니다. 서버는 글을 저장한 뒤 AI 감정 분석을 수행하고, "
                            + "분석 결과를 기반으로 몬스터와 게시글 감정 정보를 생성한 후 몬스터 등장 화면 렌더링에 필요한 응답을 반환합니다.\n\n"
                            + "글 작성 프로세스:\n"
                            + "1. 프론트가 Access Token과 함께 content, commentTone을 전송합니다.\n"
                            + "2. Spring Security가 Access Token을 검증하고 현재 사용자를 인증합니다.\n"
                            + "3. 서버가 게시글을 저장합니다.\n"
                            + "4. 저장된 게시글 본문으로 AI 감정 분석을 수행해 emotionType과 hp를 계산합니다.\n"
                            + "   hp는 최종적으로 10, 20, 30 중 하나로 결정됩니다.\n"
                            + "5. 감정 결과로 Monster를 생성하고 PostEmotion을 저장합니다.\n"
                            + "6. 게시글, 감정, 몬스터 정보를 하나의 응답으로 묶어 반환합니다.\n\n"
                            + "몬스터 유형 매핑 (monster.type):\n"
                            + "- ANXIETY_MONSTER: 불안 몬스터\n"
                            + "- LETHARGY_MONSTER: 무기력 몬스터\n"
                            + "- LONELINESS_MONSTER: 외로움 몬스터\n"
                            + "- SELF_DEPRECATION_MONSTER: 자기비하 몬스터\n"
                            + "- IRRITATION_MONSTER: 짜증 몬스터\n\n"
                            + "응답 필드 설명:\n"
                            + "- postId: 생성된 게시글 ID\n"
                            + "- author.id: 작성자 publicId (UUID)\n"
                            + "- author.jobRole: 직군 (DEVELOPMENT, DESIGN, PLANNING, MARKETING 등)\n"
                            + "- author.careerYear: 경력 (NEWCOMER, YEAR_1, YEAR_3, YEAR_5, YEAR_10_PLUS 등)\n"
                            + "- commentTone: 댓글 톤 (VENT_WITH_ME=대신 욕해주기, COMFORT_ME=무조건 위로해주기, WARM_ADVICE=따뜻한 조언해주기, MAKE_ME_LAUGH=웃겨주기)\n"
                            + "- emotion.type: AI가 분석한 감정 타입 (ANXIETY, LETHARGY, LONELINESS, SELF_DEPRECATION, IRRITATION)\n"
                            + "- emotion.displayName: 감정 한국어 표시명\n"
                            + "- emotion.summary: AI가 생성한 감정 한 줄 요약\n"
                            + "- monster.hp / monster.maxHp: 몬스터 현재 HP와 최대 HP (hp는 10·20·30 중 하나)\n"
                            + "- monster.status: 몬스터 상태 (ALIVE=살아있음, DEAD=처치됨)\n"
                            + "- likeCount / commentCount: 좋아요 수 / 댓글 수 (작성 직후는 항상 0)\n\n"
                            + "프론트는 이 응답만으로 작성 완료 화면과 몬스터 등장 화면을 바로 렌더링할 수 있습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description =
                        "게시글 작성 성공. 게시글 저장, AI 감정 분석, 몬스터 생성, 게시글 감정 저장까지 완료된 상태의 응답을 반환합니다. "
                                + "monster.type은 프론트 에셋 선택용 값이며 "
                                + "ANXIETY_MONSTER=불안, LETHARGY_MONSTER=무기력, "
                                + "LONELINESS_MONSTER=외로움, SELF_DEPRECATION_MONSTER=자기비하, "
                                + "IRRITATION_MONSTER=짜증을 의미합니다.",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "게시글이 작성되었습니다.",
                          "data": {
                            "postId": 1,
                            "author": { "id": "01939b10-7b0f-7c8f-9a2b-111111111111", "nickname": "ogu", "jobRole": "DEVELOPMENT", "careerYear": "YEAR_3" },
                            "content": "면접에서 계속 떨어져서 점점 자신감이 사라져요.",
                            "commentTone": "COMFORT_ME",
                            "emotion": { "type": "ANXIETY", "displayName": "불안", "summary": "면접 불합격으로 인한 자신감 저하" },
                            "monster": { "type": "ANXIETY_MONSTER", "hp": 30, "maxHp": 30, "status": "ALIVE" },
                            "likeCount": 0,
                            "commentCount": 0,
                            "createdAt": "2026-04-27T21:00:00"
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "요청 유효성 검증 실패",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "유효하지 않은 요청입니다.",
                          "errors": [
                            { "field": "content", "reason": "게시글 내용은 필수입니다." },
                            { "field": "commentTone", "reason": "댓글 톤은 필수입니다." }
                          ],
                          "timestamp": "2026-05-20T21:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패 또는 Access Token 누락",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "인증이 필요합니다.",
                          "timestamp": "2026-05-20T21:00:00"
                        }
                        """)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PostCreateResponse>> createPost(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            description =
                                    "고민글 본문과 원하는 댓글 톤을 전달합니다. "
                                            + "요청이 들어오면 서버는 게시글 저장, AI 감정 분석, 몬스터 생성, 감정 저장까지 순차적으로 처리합니다. "
                                            + "content는 최대 500자까지 입력할 수 있습니다. "
                                            + "commentTone은 VENT_WITH_ME(대신 욕해주기), COMFORT_ME(무조건 위로해주기), "
                                            + "WARM_ADVICE(따뜻한 조언해주기), MAKE_ME_LAUGH(웃겨주기) 중 하나입니다.",
                            required = true)
                    @RequestBody
                    @Valid
                    PostCreateRequest request) {
        PostCreateResponse response = postService.addPost(principal.publicId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("게시글이 작성되었습니다.", response));
    }

    @Operation(
            summary = "게시글 목록 조회",
            description =
                    "게시글 목록을 커서 기반으로 조회합니다. "
                            + "order로 최신순(LATEST) 또는 인기순(POPULAR)을 선택할 수 있습니다. "
                            + "jobRole과 careerYear는 반복 쿼리 파라미터로 다중 선택을 지원하며, "
                            + "작성자의 현재 프로필 정보(users.job_type, users.career_level)를 기준으로 필터링합니다. "
                            + "같은 필터 그룹 안에서는 OR 조건, 직군과 경력 조건 사이에서는 AND 조건으로 조합합니다. "
                            + "전체 선택 상태는 해당 파라미터를 생략합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "게시글 목록 조회 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "게시글 목록을 조회했습니다.",
                          "data": {
                            "posts": [
                              {
                                "postId": 1,
                                "authorNickname": "ogu",
                                "jobRole": "DEVELOPMENT",
                                "careerYear": "YEAR_3",
                                "contentPreview": "면접에서 계속 떨어져서 점점 자신감이...",
                                "emotionType": "ANXIETY",
                                "monster": { "type": "ANXIETY_MONSTER", "hp": 80, "maxHp": 100, "status": "ALIVE" },
                                "likeCount": 3,
                                "likedByMe": true,
                                "commentCount": 5,
                                "createdAt": "2026-04-27T21:00:00"
                              }
                            ],
                            "nextCursor": null
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @GetMapping
    public ApiResponse<PostListResponse> getPosts(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "커서 (마지막 postId)") @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 방식. LATEST=최신순, POPULAR=인기순")
                    @RequestParam(defaultValue = "LATEST")
                    String order,
            @Parameter(
                            description =
                                    "직군 필터. 반복 파라미터로 다중 선택 가능. "
                                            + "예: jobRole=PLANNING&jobRole=DESIGN. "
                                            + "허용 값: PLANNING(기획), DESIGN(디자인), DEVELOPMENT(개발), "
                                            + "MARKETING(마케팅), SALES(영업), HR(인사), "
                                            + "GENERAL_AFFAIRS(총무), PRODUCTION(생산), ACCOUNTING(회계), OTHER(기타)")
                    @RequestParam(required = false)
                    List<String> jobRole,
            @Parameter(
                            description =
                                    "경력 필터. 반복 파라미터로 다중 선택 가능. "
                                            + "예: careerYear=YEAR_1&careerYear=YEAR_3. "
                                            + "허용 값: NEWCOMER(신입), YEAR_1(1년차), YEAR_2(2년차), "
                                            + "YEAR_3(3년차), YEAR_4(4년차), YEAR_5(5년차), "
                                            + "YEAR_6(6년차), YEAR_7_PLUS(7년차 이상)")
                    @RequestParam(required = false)
                    List<String> careerYear) {
        return ApiResponse.ok(
                "게시글 목록을 조회했습니다.",
                postService.getPosts(
                        principal != null ? principal.publicId() : null,
                        cursor,
                        size,
                        new PostSearchCondition(jobRole, careerYear, PostOrder.from(order))));
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 본문과 댓글 목록을 함께 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "게시글 상세 조회 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "게시글을 조회했습니다.",
                          "data": {
                            "postId": 1,
                            "author": { "id": "01939b10-7b0f-7c8f-9a2b-111111111111", "nickname": "ogu", "jobRole": "DEVELOPMENT", "careerYear": "YEAR_3" },
                            "content": "면접에서 계속 떨어져서 점점 자신감이 사라져요.",
                            "commentTone": "COMFORT_ME",
                            "emotion": { "type": "ANXIETY", "displayName": "불안" },
                            "monster": { "type": "ANXIETY_MONSTER", "hp": 80, "maxHp": 100, "status": "ALIVE" },
                            "likeCount": 3,
                            "likedByMe": true,
                            "commentCount": 1,
                            "comments": [
                              {
                                "commentId": 1,
                                "authorId": "01939b10-7b0f-7c8f-9a2b-222222222222",
                                "authorNickname": "anonymous",
                                "jobRole": "PLANNING",
                                "careerYear": "YEAR_1",
                                "content": "힘내세요!",
                                "likeCount": 2,
                                "likedByMe": false,
                                "createdAt": "2026-04-27T21:30:00"
                              }
                            ],
                            "createdAt": "2026-04-27T21:00:00"
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> getPost(
            @AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long postId) {
        return ApiResponse.ok(
                "게시글을 조회했습니다.",
                postService.getPostDetail(principal != null ? principal.publicId() : null, postId));
    }

    @Operation(summary = "게시글 수정")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "게시글 수정 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "게시글이 수정되었습니다.",
                          "data": {
                            "postId": 1,
                            "author": { "id": "01939b10-7b0f-7c8f-9a2b-111111111111", "nickname": "ogu", "jobRole": "DEVELOPMENT", "careerYear": "YEAR_3" },
                            "content": "수정된 내용입니다.",
                            "commentTone": "COMFORT_ME",
                            "emotion": { "type": "ANXIETY", "displayName": "불안", "summary": "면접 불합격으로 인한 자신감 저하" },
                            "monster": { "type": "ANXIETY_MONSTER", "hp": 80, "maxHp": 100, "status": "ALIVE" },
                            "likeCount": 3,
                            "commentCount": 5,
                            "createdAt": "2026-04-27T21:00:00"
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @PatchMapping("/{postId}")
    public ApiResponse<PostResponse> updatePost(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long postId,
            @RequestBody @Valid PostCreateRequest request) {
        return ApiResponse.ok(
                "게시글이 수정되었습니다.", postService.modifyPost(principal.publicId(), postId, request));
    }

    @Operation(summary = "게시글 삭제")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "204",
                description = "게시글 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패 또는 Access Token 누락",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "인증이 필요합니다.",
                          "timestamp": "2026-05-20T21:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "본인 게시글이 아니라 삭제할 수 없음",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "권한이 없습니다.",
                          "timestamp": "2026-05-20T21:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "존재하지 않거나 이미 삭제된 게시글",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "존재하지 않는 게시글입니다.",
                          "timestamp": "2026-05-20T21:00:00"
                        }
                        """)))
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long postId) {
        postService.deletePost(principal.publicId(), postId);
        return ResponseEntity.noContent().build();
    }
}
