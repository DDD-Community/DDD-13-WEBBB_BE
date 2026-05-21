package com.ddd.webbb.post.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import com.ddd.webbb.post.application.PostService;
import com.ddd.webbb.post.domain.CommentTone;
import com.ddd.webbb.post.interfaces.dto.PostCreateRequest;
import com.ddd.webbb.post.interfaces.dto.PostCreateResponse;
import com.ddd.webbb.user.application.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PostController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class PostControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PostService postService;
    @MockitoBean private UserService userService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockitoBean private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    @MockitoBean private RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Test
    void 정상_글_작성은_201을_반환한다() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
        PostCreateResponse response =
                new PostCreateResponse(
                        1L,
                        new PostCreateResponse.AuthorInfo(
                                userId.toString(), "ogu", "DEVELOPMENT", "YEAR_3"),
                        "면접에서 계속 떨어져서 점점 자신감이 사라져요.",
                        CommentTone.COMFORT_ME,
                        new PostCreateResponse.EmotionInfo("ANXIETY", "불안", "걱정과 긴장으로 마음이 무거운 상태"),
                        new PostCreateResponse.MonsterInfo("ANXIETY_MONSTER", 30, 30, "ALIVE"),
                        0,
                        0,
                        LocalDateTime.of(2026, 5, 20, 22, 0));

        given(postService.addPost(eq(userId), any(PostCreateRequest.class))).willReturn(response);

        mockMvc.perform(
                        post("/api/posts")
                                .with(
                                        authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        principal, null, List.of())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {
                      "content": "면접에서 계속 떨어져서 점점 자신감이 사라져요.",
                      "commentTone": "COMFORT_ME"
                    }
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글이 작성되었습니다."))
                .andExpect(jsonPath("$.data.postId").value(1L))
                .andExpect(jsonPath("$.data.commentTone").value("COMFORT_ME"))
                .andExpect(jsonPath("$.data.emotion.type").value("ANXIETY"))
                .andExpect(jsonPath("$.data.monster.type").value("ANXIETY_MONSTER"));
    }

    @Test
    void 미인증_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {
                      "content": "면접에서 계속 떨어져서 점점 자신감이 사라져요.",
                      "commentTone": "COMFORT_ME"
                    }
                    """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void content_누락은_400을_반환한다() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");

        mockMvc.perform(
                        post("/api/posts")
                                .with(
                                        authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        principal, null, List.of())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {
                      "content": "",
                      "commentTone": "COMFORT_ME"
                    }
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 잘못된_commentTone은_400을_반환한다() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");

        mockMvc.perform(
                        post("/api/posts")
                                .with(
                                        authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        principal, null, List.of())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {
                      "content": "정말 힘들어요.",
                      "commentTone": "NOT_A_TONE"
                    }
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void content가_500자를_초과하면_400을_반환한다() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
        String overLimitContent = StringUtils.repeat("가", 501);

        mockMvc.perform(
                        post("/api/posts")
                                .with(
                                        authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        principal, null, List.of())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {
                      "content": "%s",
                      "commentTone": "COMFORT_ME"
                    }
                    """
                                                .formatted(overLimitContent)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 작성자가_게시글을_삭제하면_204를_반환한다() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");

        mockMvc.perform(
                        delete("/api/posts/{postId}", 1L)
                                .with(
                                        authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        principal, null, List.of()))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(postService).deletePost(userId, 1L);
    }

    @Test
    void 미인증_삭제_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/posts/{postId}", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 타인_게시글_삭제는_403을_반환한다() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
        willThrow(new AppException(ErrorCode.FORBIDDEN)).given(postService).deletePost(userId, 1L);

        mockMvc.perform(
                        delete("/api/posts/{postId}", 1L)
                                .with(
                                        authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        principal, null, List.of()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("권한이 없습니다."));
    }

    @Test
    void 없는_게시글_삭제는_404를_반환한다() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");
        willThrow(new AppException(ErrorCode.POST_NOT_FOUND))
                .given(postService)
                .deletePost(userId, 99L);

        mockMvc.perform(
                        delete("/api/posts/{postId}", 99L)
                                .with(
                                        authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        principal, null, List.of()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시글입니다."));
    }
}
