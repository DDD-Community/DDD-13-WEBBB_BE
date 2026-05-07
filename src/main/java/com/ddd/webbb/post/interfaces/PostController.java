package com.ddd.webbb.post.interfaces;

import com.ddd.webbb.global.common.response.ApiResponse;
import com.ddd.webbb.post.interfaces.dto.PostCreateRequest;
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
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private static final PostResponse.AuthorInfo STUB_AUTHOR =
            new PostResponse.AuthorInfo(
                    "01939b10-7b0f-7c8f-9a2b-111111111111", "ogu", "DEVELOPMENT", "YEAR_3");

    private static final PostResponse.EmotionInfo STUB_EMOTION =
            new PostResponse.EmotionInfo("ANXIETY", "불안", "면접 불합격으로 인한 자신감 저하");

    private static final PostResponse.MonsterInfo STUB_MONSTER =
            new PostResponse.MonsterInfo("ANXIETY_MONSTER", 80, 100, "ALIVE");

    @Operation(summary = "게시글 작성")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "게시글 작성 성공",
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
                            "monster": { "type": "ANXIETY_MONSTER", "hp": 80, "maxHp": 100, "status": "ALIVE" },
                            "likeCount": 0,
                            "commentCount": 0,
                            "createdAt": "2026-04-27T21:00:00"
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestBody @Valid PostCreateRequest request) {
        // TODO: 실제 서비스 연동
        PostResponse response =
                new PostResponse(
                        1L,
                        STUB_AUTHOR,
                        request.content(),
                        request.commentTone(),
                        STUB_EMOTION,
                        STUB_MONSTER,
                        0,
                        0,
                        LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("게시글이 작성되었습니다.", response));
    }

    @Operation(summary = "게시글 목록 조회")
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
            @Parameter(description = "커서 (마지막 postId)") @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        // TODO: 실제 서비스 연동
        PostListResponse.MonsterInfo monsterInfo =
                new PostListResponse.MonsterInfo("ANXIETY_MONSTER", 80, 100, "ALIVE");
        PostListResponse.PostSummary summary =
                new PostListResponse.PostSummary(
                        1L,
                        "ogu",
                        "DEVELOPMENT",
                        "YEAR_3",
                        "면접에서 계속 떨어져서 점점 자신감이...",
                        "ANXIETY",
                        monsterInfo,
                        3,
                        5,
                        LocalDateTime.now());
        return ApiResponse.ok("게시글 목록을 조회했습니다.", new PostListResponse(List.of(summary), null));
    }

    @Operation(summary = "게시글 상세 조회")
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
                            "commentCount": 1,
                            "comments": [
                              { "commentId": 1, "authorNickname": "anonymous", "content": "힘내세요!", "createdAt": "2026-04-27T21:30:00" }
                            ],
                            "createdAt": "2026-04-27T21:00:00"
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @GetMapping("/{postId}")
    public ApiResponse<PostDetailResponse> getPost(@PathVariable Long postId) {
        // TODO: 실제 서비스 연동
        PostDetailResponse.AuthorInfo author =
                new PostDetailResponse.AuthorInfo(
                        "01939b10-7b0f-7c8f-9a2b-111111111111", "ogu", "DEVELOPMENT", "YEAR_3");
        PostDetailResponse.EmotionInfo emotion =
                new PostDetailResponse.EmotionInfo("ANXIETY", "불안");
        PostDetailResponse.MonsterInfo monster =
                new PostDetailResponse.MonsterInfo("ANXIETY_MONSTER", 80, 100, "ALIVE");
        PostDetailResponse.CommentInfo comment =
                new PostDetailResponse.CommentInfo(1L, "anonymous", "힘내세요!", LocalDateTime.now());
        PostDetailResponse response =
                new PostDetailResponse(
                        postId,
                        author,
                        "면접에서 계속 떨어져서 점점 자신감이 사라져요.",
                        "COMFORT_ME",
                        emotion,
                        monster,
                        3,
                        1,
                        List.of(comment),
                        LocalDateTime.now());
        return ApiResponse.ok("게시글을 조회했습니다.", response);
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
            @PathVariable Long postId, @RequestBody @Valid PostCreateRequest request) {
        // TODO: 실제 서비스 연동
        PostResponse response =
                new PostResponse(
                        postId,
                        STUB_AUTHOR,
                        request.content(),
                        request.commentTone(),
                        STUB_EMOTION,
                        STUB_MONSTER,
                        3,
                        5,
                        LocalDateTime.now());
        return ApiResponse.ok("게시글이 수정되었습니다.", response);
    }

    @Operation(summary = "게시글 삭제")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "게시글 삭제 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "게시글이 삭제되었습니다.",
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @DeleteMapping("/{postId}")
    public ApiResponse<Void> deletePost(@PathVariable Long postId) {
        // TODO: 실제 서비스 연동
        return ApiResponse.ok("게시글이 삭제되었습니다.", null);
    }
}
