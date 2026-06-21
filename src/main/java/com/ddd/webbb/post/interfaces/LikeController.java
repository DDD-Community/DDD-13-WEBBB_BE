package com.ddd.webbb.post.interfaces;

import com.ddd.webbb.global.common.response.ApiResponse;
import com.ddd.webbb.global.security.CustomUserPrincipal;
import com.ddd.webbb.post.application.PostLikeService;
import com.ddd.webbb.post.interfaces.dto.LikeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Like", description = "좋아요 API")
@RestController
@RequestMapping("/api/posts/{postId}/likes")
public class LikeController {

    private final PostLikeService postLikeService;

    public LikeController(PostLikeService postLikeService) {
        this.postLikeService = postLikeService;
    }

    @Operation(summary = "좋아요 등록")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "좋아요 등록 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "좋아요를 등록했습니다.",
                          "data": {
                            "postId": 1,
                            "likeCount": 4,
                            "monster": { "hp": 70, "maxHp": 100, "status": "ALIVE" }
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @PostMapping
    public ApiResponse<LikeResponse> like(
            @AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long postId) {
        return ApiResponse.ok(
                "좋아요를 등록했습니다.", postLikeService.addPostLike(principal.publicId(), postId));
    }

    @Operation(summary = "좋아요 취소")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "좋아요 취소 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "좋아요를 취소했습니다.",
                          "data": {
                            "postId": 1,
                            "likeCount": 3,
                            "monster": { "hp": 80, "maxHp": 100, "status": "ALIVE" }
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @DeleteMapping("/me")
    public ApiResponse<LikeResponse> unlike(
            @AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long postId) {
        return ApiResponse.ok(
                "좋아요를 취소했습니다.", postLikeService.removePostLike(principal.publicId(), postId));
    }
}
