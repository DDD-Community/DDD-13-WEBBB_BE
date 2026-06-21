package com.ddd.webbb.post.interfaces;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.ddd.webbb.post.application.PostLikeService;
import com.ddd.webbb.post.interfaces.dto.LikeResponse;
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

@WebMvcTest(LikeController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class LikeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PostLikeService postLikeService;
    @MockitoBean private UserService userService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockitoBean private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    @MockitoBean private RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Nested
    @DisplayName("게시글 좋아요 등록")
    class CreatePostLike {

        @Test
        @DisplayName("정상 좋아요 → 200")
        void success() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            LikeResponse response =
                    new LikeResponse(1L, 4, new LikeResponse.MonsterInfo(29, 30, "ALIVE"));

            given(postLikeService.addPostLike(userId, 1L)).willReturn(response);

            mockMvc.perform(
                            post("/api/posts/{postId}/likes", 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.postId").value(1L))
                    .andExpect(jsonPath("$.data.likeCount").value(4))
                    .andExpect(jsonPath("$.data.monster.hp").value(29));
        }

        @Test
        @DisplayName("미인증 요청 → 401")
        void unauthorized() throws Exception {
            mockMvc.perform(post("/api/posts/{postId}/likes", 1L))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("존재하지 않는 게시글 → 404")
        void postNotFound() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            willThrow(new AppException(ErrorCode.POST_NOT_FOUND))
                    .given(postLikeService)
                    .addPostLike(userId, 99L);

            mockMvc.perform(
                            post("/api/posts/{postId}/likes", 99L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of()))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
        }

        @Test
        @DisplayName("중복 좋아요 → 409")
        void alreadyLiked() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            willThrow(new AppException(ErrorCode.ALREADY_LIKED_POST))
                    .given(postLikeService)
                    .addPostLike(userId, 1L);

            mockMvc.perform(
                            post("/api/posts/{postId}/likes", 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of()))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("이미 좋아요한 게시글입니다."));
        }
    }

    @Nested
    @DisplayName("게시글 좋아요 취소")
    class DeletePostLike {

        @Test
        @DisplayName("정상 취소 → 200")
        void success() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            LikeResponse response =
                    new LikeResponse(1L, 3, new LikeResponse.MonsterInfo(29, 30, "ALIVE"));
            given(postLikeService.removePostLike(userId, 1L)).willReturn(response);

            mockMvc.perform(
                            delete("/api/posts/{postId}/likes/me", 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("좋아요를 취소했습니다."))
                    .andExpect(jsonPath("$.data.postId").value(1L))
                    .andExpect(jsonPath("$.data.likeCount").value(3));
        }

        @Test
        @DisplayName("미인증 요청 → 401")
        void unauthorized() throws Exception {
            mockMvc.perform(delete("/api/posts/{postId}/likes/me", 1L))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("좋아요 기록 없음 → 404")
        void likeNotFound() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
            willThrow(new AppException(ErrorCode.POST_LIKE_NOT_FOUND))
                    .given(postLikeService)
                    .removePostLike(userId, 1L);

            mockMvc.perform(
                            delete("/api/posts/{postId}/likes/me", 1L)
                                    .with(
                                            authentication(
                                                    new UsernamePasswordAuthenticationToken(
                                                            principal, null, List.of()))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("좋아요 기록이 존재하지 않습니다."));
        }
    }
}
