package com.ddd.webbb.mypage.interfaces;

import com.ddd.webbb.global.common.response.ApiResponse;
import com.ddd.webbb.global.security.CustomUserPrincipal;
import com.ddd.webbb.mypage.application.MyPageService;
import com.ddd.webbb.mypage.interfaces.dto.MonsterStatsResponse;
import com.ddd.webbb.mypage.interfaces.dto.MyCommentResponse;
import com.ddd.webbb.mypage.interfaces.dto.MyPostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "MyPage", description = "마이페이지 API")
@RestController
@RequestMapping("/api/me")
public class MyPageController {

    private final MyPageService myPageService;

    public MyPageController(MyPageService myPageService) {
        this.myPageService = myPageService;
    }

    @Operation(summary = "내가 작성한 게시글 목록 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "내 게시글 목록 조회 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "내 게시글 목록을 조회했습니다.",
                          "data": {
                            "posts": [
                              {
                                "postId": 1,
                                "contentPreview": "면접에서 계속 떨어져서 점점 자신감이...",
                                "emotionType": "ANXIETY",
                                "monsterStatus": "ALIVE",
                                "createdAt": "2026-04-27T21:00:00"
                              }
                            ],
                            "nextCursor": null
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @GetMapping("/posts")
    public ApiResponse<MyPostResponse> readMyPosts(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "커서 (마지막 postId)") @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        MyPostResponse response = myPageService.getMyPosts(principal.publicId(), cursor, size);
        return ApiResponse.ok("내 게시글 목록을 조회했습니다.", response);
    }

    @Operation(summary = "내가 작성한 댓글 목록 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "내 댓글 목록 조회 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "내 댓글 목록을 조회했습니다.",
                          "data": {
                            "comments": [
                              {
                                "commentId": 1,
                                "postId": 1,
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
    @GetMapping("/comments")
    public ApiResponse<MyCommentResponse> readMyComments(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "커서 (마지막 commentId)") @RequestParam(required = false)
                    Long cursor,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        MyCommentResponse response =
                myPageService.getMyComments(principal.publicId(), cursor, size);
        return ApiResponse.ok("내 댓글 목록을 조회했습니다.", response);
    }

    @Operation(summary = "몬스터 통계 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "몬스터 통계 조회 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "몬스터 통계를 조회했습니다.",
                          "data": {
                            "totalMonsterCount": 5,
                            "defeatedMonsterCount": 3,
                            "mostFrequentEmotion": { "type": "ANXIETY", "displayName": "불안", "count": 3 }
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @GetMapping("/monster-stats")
    public ApiResponse<MonsterStatsResponse> readMonsterStats() {
        // TODO: 실제 서비스 연동
        MonsterStatsResponse.MostFrequentEmotion emotion =
                new MonsterStatsResponse.MostFrequentEmotion("ANXIETY", "불안", 3);
        return ApiResponse.ok("몬스터 통계를 조회했습니다.", new MonsterStatsResponse(5, 3, emotion));
    }
}
