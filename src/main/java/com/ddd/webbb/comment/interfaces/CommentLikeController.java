package com.ddd.webbb.comment.interfaces;

import com.ddd.webbb.comment.application.CommentLikeService;
import com.ddd.webbb.comment.interfaces.dto.CommentLikeResponse;
import com.ddd.webbb.global.common.response.ApiResponse;
import com.ddd.webbb.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Comment Like", description = "댓글 공감 API")
@RestController
@RequestMapping("/api/posts/{postId}/comments/{commentId}/likes")
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    public CommentLikeController(CommentLikeService commentLikeService) {
        this.commentLikeService = commentLikeService;
    }

    @Operation(
            summary = "댓글 공감 등록",
            description =
                    "해당 댓글에 공감합니다. "
                            + "한 사용자가 같은 댓글에 중복 공감할 수 없습니다. "
                            + "공감 등록 시 해당 게시글의 몬스터 HP가 1 감소합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "댓글 공감 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "댓글에 공감했습니다.",
                          "data": {
                            "commentId": 1,
                            "likeCount": 4,
                            "monster": { "hp": 28, "maxHp": 30, "status": "ALIVE" }
                          },
                          "timestamp": "2026-04-27T22:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "댓글이 해당 게시글에 속하지 않음",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "해당 댓글은 이 게시글에 속하지 않습니다.",
                          "timestamp": "2026-04-27T22:00:00"
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
                          "timestamp": "2026-04-27T22:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "존재하지 않는 댓글",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "존재하지 않는 댓글입니다.",
                          "timestamp": "2026-04-27T22:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "이미 공감한 댓글",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "이미 공감한 댓글입니다.",
                          "timestamp": "2026-04-27T22:00:00"
                        }
                        """)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CommentLikeResponse>> createCommentLike(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        CommentLikeResponse response =
                commentLikeService.addCommentLike(principal.publicId(), postId, commentId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("댓글에 공감했습니다.", response));
    }

    @Operation(summary = "댓글 공감 취소", description = "본인의 댓글 공감을 취소합니다. HP 복구는 없습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "댓글 공감 취소 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "댓글 공감을 취소했습니다.",
                          "timestamp": "2026-04-27T22:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "댓글이 해당 게시글에 속하지 않음",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "해당 댓글은 이 게시글에 속하지 않습니다.",
                          "timestamp": "2026-04-27T22:00:00"
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
                          "timestamp": "2026-04-27T22:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "공감 기록이 존재하지 않음",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "공감 기록이 존재하지 않습니다.",
                          "timestamp": "2026-04-27T22:00:00"
                        }
                        """)))
    })
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteCommentLike(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        commentLikeService.removeCommentLike(principal.publicId(), postId, commentId);
        return ResponseEntity.ok(ApiResponse.ok("댓글 공감을 취소했습니다.", null));
    }
}
