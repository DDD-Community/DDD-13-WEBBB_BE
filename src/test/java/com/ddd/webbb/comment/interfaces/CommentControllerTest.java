package com.ddd.webbb.comment.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddd.webbb.comment.application.CommentService;
import com.ddd.webbb.comment.interfaces.dto.CommentCreateRequest;
import com.ddd.webbb.comment.interfaces.dto.CommentListResponse;
import com.ddd.webbb.comment.interfaces.dto.CommentResponse;
import com.ddd.webbb.comment.interfaces.dto.CommentUpdateRequest;
import com.ddd.webbb.global.auth.CustomOAuth2UserService;
import com.ddd.webbb.global.auth.JwtAuthFilter;
import com.ddd.webbb.global.auth.JwtAuthenticationEntryPoint;
import com.ddd.webbb.global.auth.JwtProvider;
import com.ddd.webbb.global.auth.OAuth2LoginFailureHandler;
import com.ddd.webbb.global.auth.OAuth2LoginSuccessHandler;
import com.ddd.webbb.global.auth.RedisOAuth2AuthorizationRequestRepository;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.global.config.SecurityConfig;
import com.ddd.webbb.global.security.CustomUserPrincipal;
import com.ddd.webbb.user.application.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class CommentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CommentService commentService;
    @MockitoBean private UserService userService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockitoBean private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    @MockitoBean private RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Nested
    @DisplayName("댓글 목록 조회")
    class ReadComments {

        @Test
        @DisplayName("정상 조회 → 200")
        void success() throws Exception {
            CommentListResponse response =
                    new CommentListResponse(
                            List.of(
                                    new CommentListResponse.CommentSummary(
                                            1L,
                                            "ogu",
                                            "힘내세요!",
                                            3,
                                            List.of(
                                                    new CommentListResponse.ReplySummary(
                                                            2L,
                                                            1L,
                                                            "maru",
                                                            "저도 응원합니다!",
                                                            1,
                                                            LocalDateTime.of(2026, 4, 27, 22, 0))),
                                            LocalDateTime.of(2026, 4, 27, 21, 30))),
                            null);

            given(commentService.getComments(eq(1L), isNull(), eq(20))).willReturn(response);

            mockMvc.perform(get("/api/posts/{postId}/comments", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.comments").isArray())
                    .andExpect(jsonPath("$.data.comments[0].commentId").value(1L))
                    .andExpect(jsonPath("$.data.comments[0].replies[0].commentId").value(2L));
        }
    }

    @Nested
    @DisplayName("댓글 작성")
    class CreateComment {

        @Test
        @DisplayName("정상 작성 → 201")
        void success() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            CommentResponse response =
                    new CommentResponse(
                            1L,
                            1L,
                            null,
                            "힘내세요!",
                            new CommentResponse.MonsterInfo(27, 30, "ALIVE"),
                            LocalDateTime.of(2026, 4, 27, 21, 30));

            given(commentService.addComment(eq(userId), eq(1L), any(CommentCreateRequest.class)))
                    .willReturn(response);

            mockMvc.perform(
                            post("/api/posts/{postId}/comments", 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of())))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {"parentCommentId": null, "content": "힘내세요!"}
                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.commentId").value(1L))
                    .andExpect(jsonPath("$.data.monster.hp").value(27));
        }

        @Test
        @DisplayName("미인증 요청 → 401")
        void unauthorized() throws Exception {
            mockMvc.perform(
                            post("/api/posts/{postId}/comments", 1L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {"content": "힘내세요!"}
                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("빈 내용 → 400")
        void blankContent() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");

            mockMvc.perform(
                            post("/api/posts/{postId}/comments", 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of())))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {"content": ""}
                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 게시글 → 404")
        void postNotFound() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            willThrow(new AppException(ErrorCode.POST_NOT_FOUND))
                    .given(commentService)
                    .addComment(eq(userId), eq(99L), any(CommentCreateRequest.class));

            mockMvc.perform(
                            post("/api/posts/{postId}/comments", 99L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of())))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {"content": "힘내세요!"}
                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
        }
    }

    @Nested
    @DisplayName("댓글 수정")
    class UpdateComment {

        @Test
        @DisplayName("정상 수정 → 200")
        void success() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            CommentResponse response =
                    new CommentResponse(
                            1L,
                            1L,
                            null,
                            "수정된 내용",
                            new CommentResponse.MonsterInfo(29, 30, "ALIVE"),
                            LocalDateTime.of(2026, 4, 27, 21, 30));

            given(commentService.modifyComment(eq(userId), eq(1L), any(CommentUpdateRequest.class)))
                    .willReturn(response);

            mockMvc.perform(
                            patch("/api/comments/{commentId}", 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of())))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {"content": "수정된 내용"}
                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").value("수정된 내용"));
        }

        @Test
        @DisplayName("미인증 요청 → 401")
        void unauthorized() throws Exception {
            mockMvc.perform(
                            patch("/api/comments/{commentId}", 1L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {"content": "수정 시도"}
                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("타인 댓글 수정 → 403")
        void forbidden() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            willThrow(new AppException(ErrorCode.FORBIDDEN))
                    .given(commentService)
                    .modifyComment(eq(userId), eq(1L), any(CommentUpdateRequest.class));

            mockMvc.perform(
                            patch("/api/comments/{commentId}", 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of())))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {"content": "수정 시도"}
                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("권한이 없습니다."));
        }

        @Test
        @DisplayName("존재하지 않는 댓글 수정 → 404")
        void notFound() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            willThrow(new AppException(ErrorCode.COMMENT_NOT_FOUND))
                    .given(commentService)
                    .modifyComment(eq(userId), eq(99L), any(CommentUpdateRequest.class));

            mockMvc.perform(
                            patch("/api/comments/{commentId}", 99L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of())))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {"content": "수정 시도"}
                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("존재하지 않는 댓글입니다."));
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("정상 삭제 → 200")
        void success() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            willDoNothing().given(commentService).removeComment(userId, 1L);

            mockMvc.perform(
                            delete("/api/comments/{commentId}", 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("댓글이 삭제되었습니다."));
        }

        @Test
        @DisplayName("미인증 요청 → 401")
        void unauthorized() throws Exception {
            mockMvc.perform(delete("/api/comments/{commentId}", 1L))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("타인 댓글 삭제 → 403")
        void forbidden() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            willThrow(new AppException(ErrorCode.FORBIDDEN))
                    .given(commentService)
                    .removeComment(userId, 1L);

            mockMvc.perform(
                            delete("/api/comments/{commentId}", 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of()))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("권한이 없습니다."));
        }

        @Test
        @DisplayName("존재하지 않는 댓글 삭제 → 404")
        void notFound() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            willThrow(new AppException(ErrorCode.COMMENT_NOT_FOUND))
                    .given(commentService)
                    .removeComment(userId, 99L);

            mockMvc.perform(
                            delete("/api/comments/{commentId}", 99L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of()))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("존재하지 않는 댓글입니다."));
        }
    }
}
