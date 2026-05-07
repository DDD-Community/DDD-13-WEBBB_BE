package com.ddd.webbb.comment.interfaces;

import com.ddd.webbb.comment.interfaces.dto.CommentCreateRequest;
import com.ddd.webbb.comment.interfaces.dto.CommentListResponse;
import com.ddd.webbb.comment.interfaces.dto.CommentResponse;
import com.ddd.webbb.global.common.response.ApiResponse;
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

    @Operation(summary = "댓글 작성")
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
                            "content": "지금 많이 힘들겠지만, 여기까지 온 것만으로도 충분히 잘하고 있어요.",
                            "monster": { "hp": 70, "maxHp": 100, "status": "ALIVE" },
                            "createdAt": "2026-04-27T21:30:00"
                          },
                          "timestamp": "2026-04-27T21:30:00"
                        }
                        """)))
    })
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable Long postId, @RequestBody @Valid CommentCreateRequest request) {
        // TODO: 실제 서비스 연동
        CommentResponse.MonsterInfo monster = new CommentResponse.MonsterInfo(70, 100, "ALIVE");
        CommentResponse response =
                new CommentResponse(1L, postId, request.content(), monster, LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("댓글이 작성되었습니다.", response));
    }

    @Operation(summary = "댓글 목록 조회")
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
                                "authorNickname": "anonymous",
                                "content": "지금 많이 힘들겠지만, 여기까지 온 것만으로도 충분히 잘하고 있어요.",
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
    public ApiResponse<CommentListResponse> getComments(
            @PathVariable Long postId,
            @Parameter(description = "커서 (마지막 commentId)") @RequestParam(required = false)
                    Long cursor,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        // TODO: 실제 서비스 연동
        CommentListResponse.CommentSummary summary =
                new CommentListResponse.CommentSummary(
                        1L,
                        "anonymous",
                        "지금 많이 힘들겠지만, 여기까지 온 것만으로도 충분히 잘하고 있어요.",
                        LocalDateTime.now());
        return ApiResponse.ok("댓글 목록을 조회했습니다.", new CommentListResponse(List.of(summary), null));
    }

    @Operation(summary = "댓글 삭제")
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
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long commentId) {
        // TODO: 실제 서비스 연동
        return ApiResponse.ok("댓글이 삭제되었습니다.", null);
    }
}
