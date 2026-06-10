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
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "MyPage", description = "마이페이지 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me")
public class MyPageController {

    private final MyPageService myPageService;

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
    public ApiResponse<MyPostResponse> getMyPosts(
            @Parameter(description = "커서 (마지막 postId)") @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        // TODO: 실제 서비스 연동
        MyPostResponse.MyPost post =
                new MyPostResponse.MyPost(
                        1L, "면접에서 계속 떨어져서 점점 자신감이...", "ANXIETY", "ALIVE", LocalDateTime.now());
        return ApiResponse.ok("내 게시글 목록을 조회했습니다.", new MyPostResponse(List.of(post), null));
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
    public ApiResponse<MyCommentResponse> getMyComments(
            @Parameter(description = "커서 (마지막 commentId)") @RequestParam(required = false)
                    Long cursor,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        // TODO: 실제 서비스 연동
        MyCommentResponse.MyComment comment =
                new MyCommentResponse.MyComment(
                        1L, 1L, "지금 많이 힘들겠지만, 여기까지 온 것만으로도 충분히 잘하고 있어요.", LocalDateTime.now());
        return ApiResponse.ok("내 댓글 목록을 조회했습니다.", new MyCommentResponse(List.of(comment), null));
    }

    @Operation(
            summary = "몬스터 통계 조회",
            description =
                    "로그인한 사용자의 마이페이지 몬스터 통계를 반환합니다.\n\n"
                            + "응답 필드 설명:\n"
                            + "- totalMonsterCount: 사용자가 작성한 게시글에 생성된 전체 몬스터 수\n"
                            + "- defeatedMonsterCount: 공감·댓글로 HP가 0이 되어 처치된 몬스터 수\n"
                            + "- mostFrequentEmotion: 가장 많이 나타난 감정 유형 정보\n"
                            + "  - 몬스터가 하나도 없으면 null → 프론트에서 '두드러진 감정이 없어요' 표시\n"
                            + "  - type: 감정 enum (ANXIETY=불안, LETHARGY=무기력, LONELINESS=외로움, "
                            + "SELF_DEPRECATION=자기비하, IRRITATION=짜증)\n"
                            + "  - percentage: 전체 몬스터 중 해당 감정의 비율 (0~100, 반올림)\n\n"
                            + "인증 필요: Authorization 헤더에 Bearer Access Token을 포함해야 합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "몬스터 통계 조회 성공",
                content =
                        @Content(
                                examples = {
                                    @ExampleObject(
                                            name = "감정 데이터 있음",
                                            summary = "몬스터가 존재하고 최다 감정이 있는 경우",
                                            value =
                                                    """
                        {
                          "success": true,
                          "message": "몬스터 통계를 조회했습니다.",
                          "data": {
                            "totalMonsterCount": 5,
                            "defeatedMonsterCount": 3,
                            "mostFrequentEmotion": {
                              "type": "ANXIETY",
                              "displayName": "불안",
                              "count": 2,
                              "percentage": 40
                            }
                          },
                          "timestamp": "2026-06-11T10:00:00"
                        }
                        """),
                                    @ExampleObject(
                                            name = "감정 데이터 없음",
                                            summary = "아직 게시글을 작성하지 않아 몬스터가 없는 경우",
                                            value =
                                                    """
                        {
                          "success": true,
                          "message": "몬스터 통계를 조회했습니다.",
                          "data": {
                            "totalMonsterCount": 0,
                            "defeatedMonsterCount": 0,
                            "mostFrequentEmotion": null
                          },
                          "timestamp": "2026-06-11T10:00:00"
                        }
                        """)
                                })),
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
                          "timestamp": "2026-06-11T10:00:00"
                        }
                        """)))
    })
    @GetMapping("/monster-stats")
    public ApiResponse<MonsterStatsResponse> getMonsterStats(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(
                "몬스터 통계를 조회했습니다.", myPageService.getMonsterStats(principal.publicId()));
    }
}
