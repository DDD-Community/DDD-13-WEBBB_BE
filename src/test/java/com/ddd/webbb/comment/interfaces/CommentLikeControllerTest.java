package com.ddd.webbb.comment.interfaces;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddd.webbb.comment.application.CommentLikeService;
import com.ddd.webbb.comment.interfaces.dto.CommentLikeResponse;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentLikeController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class CommentLikeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CommentLikeService commentLikeService;
    @MockitoBean private UserService userService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockitoBean private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    @MockitoBean private RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Nested
    @DisplayName("댓글 공감 등록")
    class CreateCommentLike {

        @Test
        @DisplayName("정상 공감 → 201")
        void success() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            CommentLikeResponse response =
                    new CommentLikeResponse(
                            1L, 4, new CommentLikeResponse.MonsterInfo(28, 30, "ALIVE"));

            given(commentLikeService.addCommentLike(userId, 1L, 1L)).willReturn(response);

            mockMvc.perform(
                            post("/api/posts/{postId}/comments/{commentId}/likes", 1L, 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of()))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.commentId").value(1L))
                    .andExpect(jsonPath("$.data.likeCount").value(4))
                    .andExpect(jsonPath("$.data.monster.hp").value(28));
        }

        @Test
        @DisplayName("미인증 요청 → 401")
        void unauthorized() throws Exception {
            mockMvc.perform(post("/api/posts/{postId}/comments/{commentId}/likes", 1L, 1L))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("존재하지 않는 댓글 → 404")
        void commentNotFound() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            willThrow(new AppException(ErrorCode.COMMENT_NOT_FOUND))
                    .given(commentLikeService)
                    .addCommentLike(userId, 1L, 99L);

            mockMvc.perform(
                            post("/api/posts/{postId}/comments/{commentId}/likes", 1L, 99L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of()))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("존재하지 않는 댓글입니다."));
        }

        @Test
        @DisplayName("중복 공감 → 409")
        void alreadyLiked() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            willThrow(new AppException(ErrorCode.ALREADY_LIKED_COMMENT))
                    .given(commentLikeService)
                    .addCommentLike(userId, 1L, 1L);

            mockMvc.perform(
                            post("/api/posts/{postId}/comments/{commentId}/likes", 1L, 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of()))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("이미 공감한 댓글입니다."));
        }
    }

    @Nested
    @DisplayName("댓글 공감 취소")
    class DeleteCommentLike {

        @Test
        @DisplayName("정상 취소 → 200")
        void success() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            willDoNothing().given(commentLikeService).removeCommentLike(userId, 1L, 1L);

            mockMvc.perform(
                            delete("/api/posts/{postId}/comments/{commentId}/likes/me", 1L, 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("댓글 공감을 취소했습니다."));
        }

        @Test
        @DisplayName("미인증 요청 → 401")
        void unauthorized() throws Exception {
            mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}/likes/me", 1L, 1L))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("공감 기록 없음 → 404")
        void likeNotFound() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            willThrow(new AppException(ErrorCode.COMMENT_LIKE_NOT_FOUND))
                    .given(commentLikeService)
                    .removeCommentLike(userId, 1L, 1L);

            mockMvc.perform(
                            delete("/api/posts/{postId}/comments/{commentId}/likes/me", 1L, 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of()))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("공감 기록이 존재하지 않습니다."));
        }
    }
}
