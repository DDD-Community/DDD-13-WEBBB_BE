package com.ddd.webbb.mypage.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddd.webbb.global.auth.CustomOAuth2UserService;
import com.ddd.webbb.global.auth.JwtAuthFilter;
import com.ddd.webbb.global.auth.JwtAuthenticationEntryPoint;
import com.ddd.webbb.global.auth.JwtProvider;
import com.ddd.webbb.global.auth.OAuth2LoginFailureHandler;
import com.ddd.webbb.global.auth.OAuth2LoginSuccessHandler;
import com.ddd.webbb.global.auth.RedisOAuth2AuthorizationRequestRepository;
import com.ddd.webbb.global.config.SecurityConfig;
import com.ddd.webbb.global.security.CustomUserPrincipal;
import com.ddd.webbb.mypage.application.MyPageService;
import com.ddd.webbb.mypage.interfaces.dto.MonsterStatsResponse;
import com.ddd.webbb.mypage.interfaces.dto.MyLikedPostResponse;
import com.ddd.webbb.user.application.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MyPageController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class MyPageControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private MyPageService myPageService;
    @MockitoBean private UserService userService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockitoBean private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    @MockitoBean private RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Test
    @DisplayName("인증된 사용자가 몬스터 통계를 조회한다")
    void authenticatedUser_getsMonsterStats() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "오구");

        MonsterStatsResponse response =
                new MonsterStatsResponse(
                        5, 3, new MonsterStatsResponse.MostFrequentEmotion("ANXIETY", "불안", 2, 40));
        given(myPageService.getMonsterStats(any(UUID.class))).willReturn(response);

        mockMvc.perform(
                        get("/api/me/monster-stats")
                                .with(
                                        authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        principal, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalMonsterCount").value(5))
                .andExpect(jsonPath("$.data.defeatedMonsterCount").value(3))
                .andExpect(jsonPath("$.data.mostFrequentEmotion.type").value("ANXIETY"))
                .andExpect(jsonPath("$.data.mostFrequentEmotion.displayName").value("불안"))
                .andExpect(jsonPath("$.data.mostFrequentEmotion.percentage").value(40));
    }

    @Test
    @DisplayName("인증 없이 조회하면 401을 반환한다")
    void unauthenticatedRequest_returns401() throws Exception {
        mockMvc.perform(get("/api/me/monster-stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("인증된 사용자가 공감한 게시글 목록을 조회한다")
    void authenticatedUser_getsLikedPosts() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "오구");

        MyLikedPostResponse.LikedPost likedPost =
                new MyLikedPostResponse.LikedPost(
                        1L,
                        "이직 준비 시작하는 게 진짜...",
                        "오오",
                        "개발",
                        "1년차",
                        "LETHARGY",
                        "ALIVE",
                        20,
                        30,
                        4,
                        4,
                        "COMFORT_ME",
                        LocalDateTime.of(2026, 6, 22, 12, 0));
        MyLikedPostResponse response = new MyLikedPostResponse(List.of(likedPost), null);
        given(myPageService.getLikedPosts(any(UUID.class), any(), anyInt())).willReturn(response);

        mockMvc.perform(
                        get("/api/me/liked-posts")
                                .with(
                                        authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        principal, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.posts[0].postId").value(1))
                .andExpect(jsonPath("$.data.posts[0].authorNickname").value("오오"))
                .andExpect(jsonPath("$.data.posts[0].emotionType").value("LETHARGY"))
                .andExpect(jsonPath("$.data.posts[0].currentHp").value(20))
                .andExpect(jsonPath("$.data.posts[0].maxHp").value(30))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    @Test
    @DisplayName("인증 없이 공감한 글을 조회하면 401을 반환한다")
    void unauthenticatedLikedPosts_returns401() throws Exception {
        mockMvc.perform(get("/api/me/liked-posts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
