package com.ddd.webbb.notification.interfaces;

import com.ddd.webbb.global.common.response.ApiResponse;
import com.ddd.webbb.global.security.CustomUserPrincipal;
import com.ddd.webbb.notification.application.NotificationService;
import com.ddd.webbb.notification.infrastructure.SseEmitterManager;
import com.ddd.webbb.notification.interfaces.dto.NotificationResponse;
import com.ddd.webbb.notification.interfaces.dto.UnreadNotificationResponse;
import com.ddd.webbb.user.application.UserService;
import com.ddd.webbb.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterManager sseEmitterManager;
    private final UserService userService;

    public NotificationController(
            NotificationService notificationService,
            SseEmitterManager sseEmitterManager,
            UserService userService) {
        this.notificationService = notificationService;
        this.sseEmitterManager = sseEmitterManager;
        this.userService = userService;
    }

    @Operation(
            summary = "SSE 구독",
            description =
                    "실시간 알림 수신을 위한 SSE(Server-Sent Events) 연결을 맺습니다.\n\n"
                            + "웹 페이지 진입 시 1회 호출하면 서버와 연결이 유지되며, 새 알림이 발생하는 즉시 클라이언트로 푸시됩니다.\n\n"
                            + "연결 직후 더미 이벤트가 1회 전송됩니다:\n"
                            + "```\nevent: connect\ndata: connected\n```\n\n"
                            + "이후 알림 발생 시 아래 형식으로 수신됩니다:\n"
                            + "```\nevent: notification\ndata: {\"id\":1,\"type\":\"COMMENT\",...}\n```\n\n"
                            + "**type 값별 메시지:**\n"
                            + "- `COMMENT`: {actorNickname} 님이 내 글에 댓글을 달았어요.\n"
                            + "- `POST_LIKE`: {actorNickname} 님이 내 글에 공감했어요.\n"
                            + "- `MONSTER_DEFEATED`: 내 글의 몬스터 처치를 완료했어요. (actorNickname은 null)\n\n"
                            + "**주의:** 브라우저 기본 `EventSource`는 Authorization 헤더를 지원하지 않으므로 "
                            + "`@microsoft/fetch-event-source` 라이브러리 사용을 권장합니다.\n\n"
                            + "연결 timeout은 30분이며, 연결이 끊기면 재연결이 필요합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "SSE 연결 성공 (text/event-stream)",
                content =
                        @Content(
                                mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                                                        event: connect
                                                        data: connected

                                                        event: notification
                                                        data: {"id":1,"type":"COMMENT","actorNickname":"DDD","postId":123,"isRead":false,"createdAt":"2026-06-23T20:00:00"}

                                                        event: notification
                                                        data: {"id":2,"type":"MONSTER_DEFEATED","actorNickname":null,"postId":123,"isRead":false,"createdAt":"2026-06-23T20:01:00"}
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
                                                          "timestamp": "2026-06-23T20:00:00"
                                                        }
                                                        """)))
    })
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserPrincipal principal) {
        User user = userService.getUserEntity(principal.publicId());
        return sseEmitterManager.subscribe(user.getId());
    }

    @Operation(
            summary = "알림 목록 조회",
            description =
                    "로그인한 사용자의 알림 목록을 최신순으로 반환합니다.\n\n"
                            + "페이지 로드 시 초기 데이터를 가져오는 용도로 사용합니다. "
                            + "이후 실시간 알림은 SSE를 통해 수신됩니다.\n\n"
                            + "isRead가 false인 항목이 읽지 않은 알림입니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "알림 목록 조회 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                                                        {
                                                          "success": true,
                                                          "message": "알림 목록을 조회했습니다.",
                                                          "data": [
                                                            {
                                                              "id": 3,
                                                              "type": "MONSTER_DEFEATED",
                                                              "actorNickname": null,
                                                              "postId": 123,
                                                              "isRead": false,
                                                              "createdAt": "2026-06-23T20:01:00"
                                                            },
                                                            {
                                                              "id": 2,
                                                              "type": "POST_LIKE",
                                                              "actorNickname": "maru",
                                                              "postId": 123,
                                                              "isRead": true,
                                                              "createdAt": "2026-06-23T19:30:00"
                                                            },
                                                            {
                                                              "id": 1,
                                                              "type": "COMMENT",
                                                              "actorNickname": "DDD",
                                                              "postId": 123,
                                                              "isRead": true,
                                                              "createdAt": "2026-06-23T19:00:00"
                                                            }
                                                          ],
                                                          "timestamp": "2026-06-23T20:05:00"
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
                                                          "timestamp": "2026-06-23T20:05:00"
                                                        }
                                                        """)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> readNotifications(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        List<NotificationResponse> notifications =
                notificationService.getNotifications(principal.publicId());
        return ResponseEntity.ok(ApiResponse.ok("알림 목록을 조회했습니다.", notifications));
    }

    @Operation(
            summary = "알림 읽음 처리",
            description =
                    "알림을 클릭해 해당 게시글로 이동할 때 호출합니다.\n\n" + "isRead가 이미 true인 알림에 중복 호출해도 무방합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "읽음 처리 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                                                        {
                                                          "success": true,
                                                          "message": "알림을 읽음 처리했습니다.",
                                                          "timestamp": "2026-06-23T20:05:00"
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
                                                          "timestamp": "2026-06-23T20:05:00"
                                                        }
                                                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "본인 알림이 아님",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                                                        {
                                                          "success": false,
                                                          "message": "권한이 없습니다.",
                                                          "timestamp": "2026-06-23T20:05:00"
                                                        }
                                                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "존재하지 않는 알림",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                                                        {
                                                          "success": false,
                                                          "message": "존재하지 않는 알림입니다.",
                                                          "timestamp": "2026-06-23T20:05:00"
                                                        }
                                                        """)))
    })
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> updateNotificationRead(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long notificationId) {
        notificationService.markAsRead(principal.publicId(), notificationId);
        return ResponseEntity.ok(ApiResponse.ok("알림을 읽음 처리했습니다.", null));
    }

    @Operation(
            summary = "미읽음 알림 여부 조회",
            description =
                    "읽지 않은 알림이 하나라도 있으면 true를 반환합니다.\n\n"
                            + "페이지 최초 로드 시 종 아이콘의 빨간 뱃지 표시 여부를 결정하는 데 사용합니다. "
                            + "이후 실시간 뱃지 갱신은 SSE 이벤트 수신으로 처리합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "미읽음 여부 조회 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                                                        {
                                                          "success": true,
                                                          "message": "요청이 성공했습니다.",
                                                          "data": { "hasUnread": true },
                                                          "timestamp": "2026-06-23T20:05:00"
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
                                                          "timestamp": "2026-06-23T20:05:00"
                                                        }
                                                        """)))
    })
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<UnreadNotificationResponse>> readUnreadStatus(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        UnreadNotificationResponse response = notificationService.hasUnread(principal.publicId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
