package com.ddd.webbb.comment.interfaces;

import com.ddd.webbb.comment.application.CommentService;
import com.ddd.webbb.comment.interfaces.dto.CommentCreateRequest;
import com.ddd.webbb.comment.interfaces.dto.CommentListResponse;
import com.ddd.webbb.comment.interfaces.dto.CommentResponse;
import com.ddd.webbb.comment.interfaces.dto.CommentUpdateRequest;
import com.ddd.webbb.global.common.response.ApiResponse;
import com.ddd.webbb.global.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@Tag(name = "Comment", description = "댓글 API")
@RestController
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(
            summary = "댓글 목록 조회",
            description =
                    "게시글의 댓글 목록을 커서 기반 페이지네이션으로 조회합니다. "
                            + "루트 댓글과 각 루트 댓글에 속한 대댓글(1단계)이 함께 반환됩니다. "
                            + "인증 없이 조회 가능합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "댓글 목록 조회 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "댓글 목록을 조회했습니다.",
                          "data": {
                            "comments": [
                              {
                                "commentId": 1,
                                "authorNickname": "ogu",
                                "content": "힘내세요! 잘 될 거예요.",
                                "likeCount": 3,
                                "replies": [
                                  {
                                    "commentId": 2,
                                    "parentCommentId": 1,
                                    "authorNickname": "maru",
                                    "content": "저도 응원합니다!",
                                    "likeCount": 1,
                                    "createdAt": "2026-04-27T22:00:00"
                                  }
                                ],
                                "createdAt": "2026-04-27T21:30:00"
                              }
                            ],
                            "nextCursor": null
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentListResponse>> readComments(
            @PathVariable Long postId,
            @Parameter(description = "커서 (마지막 commentId)") @RequestParam(required = false)
                    Long cursor,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        CommentListResponse response = commentService.getComments(postId, cursor, size);
        return ResponseEntity.ok(ApiResponse.ok("댓글 목록을 조회했습니다.", response));
    }

    @Operation(
            summary = "댓글 작성",
            description =
                    "게시글에 댓글 또는 대댓글을 작성합니다. "
                            + "parentCommentId를 지정하면 해당 댓글의 대댓글로 등록됩니다. "
                            + "대댓글은 1단계까지만 허용됩니다(대댓글의 대댓글 불가). "
                            + "댓글 작성 시 해당 게시글의 몬스터 HP가 1 감소합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "댓글 작성 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "댓글이 작성되었습니다.",
                          "data": {
                            "commentId": 1,
                            "postId": 1,
                            "parentCommentId": null,
                            "content": "힘내세요! 잘 될 거예요.",
                            "monster": { "hp": 29, "maxHp": 30, "status": "ALIVE" },
                            "createdAt": "2026-04-27T21:30:00"
                          },
                          "timestamp": "2026-04-27T21:30:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 요청 (빈 내용, 잘못된 부모 댓글 등)",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "부모 댓글이 해당 게시글에 속하지 않거나 대댓글에는 답글을 달 수 없습니다.",
                          "timestamp": "2026-04-27T21:30:00"
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
                          "timestamp": "2026-04-27T21:30:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "게시글 또는 부모 댓글이 존재하지 않음",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "존재하지 않는 게시글입니다.",
                          "timestamp": "2026-04-27T21:30:00"
                        }
                        """)))
    })
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long postId,
            @RequestBody @Valid CommentCreateRequest request) {
        CommentResponse response = commentService.addComment(principal.publicId(), postId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("댓글이 작성되었습니다.", response));
    }

    @Operation(summary = "댓글 수정", description = "본인이 작성한 댓글의 내용을 수정합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "댓글 수정 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "댓글이 수정되었습니다.",
                          "data": {
                            "commentId": 1,
                            "postId": 1,
                            "parentCommentId": null,
                            "content": "수정된 댓글 내용입니다.",
                            "monster": { "hp": 29, "maxHp": 30, "status": "ALIVE" },
                            "createdAt": "2026-04-27T21:30:00"
                          },
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
                responseCode = "403",
                description = "본인 댓글이 아니라 수정할 수 없음",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "권한이 없습니다.",
                          "timestamp": "2026-04-27T22:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "존재하지 않거나 이미 삭제된 댓글",
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
                        """)))
    })
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentUpdateRequest request) {
        CommentResponse response =
                commentService.modifyComment(principal.publicId(), commentId, request);
        return ResponseEntity.ok(ApiResponse.ok("댓글이 수정되었습니다.", response));
    }

    @Operation(summary = "댓글 삭제", description = "본인이 작성한 댓글을 소프트 삭제합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "댓글 삭제 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "댓글이 삭제되었습니다.",
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
                responseCode = "403",
                description = "본인 댓글이 아니라 삭제할 수 없음",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "권한이 없습니다.",
                          "timestamp": "2026-04-27T22:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "존재하지 않거나 이미 삭제된 댓글",
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
                        """)))
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long commentId) {
        commentService.removeComment(principal.publicId(), commentId);
        return ResponseEntity.ok(ApiResponse.ok("댓글이 삭제되었습니다.", null));
    }
}
