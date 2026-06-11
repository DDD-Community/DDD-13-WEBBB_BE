package com.ddd.webbb.user.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddd.webbb.auth.infrastructure.RefreshTokenStore;
import com.ddd.webbb.config.TestRedisConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRedisConfig.class)
@Transactional
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private RefreshTokenStore refreshTokenStore;

    @Nested
    @DisplayName("GET /api/users/nickname/check")
    class NicknameCheck {

        @Test
        @DisplayName("사용 가능한 닉네임 → available: true")
        void available() throws Exception {
            mockMvc.perform(get("/api/users/nickname/check").param("value", "unused"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.available").value(true));
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임 → available: false")
        void alreadyTaken() throws Exception {
            signup("taken@test.com", "ogu");

            mockMvc.perform(get("/api/users/nickname/check").param("value", "ogu"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.available").value(false));
        }

        @Test
        @DisplayName("인증 없이도 조회 가능 → 200")
        void accessibleWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/users/nickname/check").param("value", "anyname"))
                    .andExpect(status().isOk());
        }
    }

    private void signup(String email, String nickname) throws Exception {
        mockMvc.perform(
                post("/api/auth/signup/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                        {"email": "%s", "password": "password123!", "nickname": "%s"}
                        """
                                        .formatted(email, nickname)));
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.ddd.webbb.user.application.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtAuthenticationEntryPoint.class})
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockitoBean private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    @MockitoBean private RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Test
    void 회원정보수정은_허용되지_않은_직군과_경력_코드를_거부한다() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");

        mockMvc.perform(
                        patch("/api/users/{id}", userId)
                                .with(
                                        authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        principal, null, List.of())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {
                      "nickname": "ogu",
                      "jobType": "BACKEND",
                      "careerLevel": "3년차"
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 요청입니다."));
    }

    @Test
    void 내프로필수정은_허용되지_않은_직군과_경력_코드를_거부한다() throws Exception {
        UUID userId = UUID.randomUUID();
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "ogu@test.com", "ogu");

        mockMvc.perform(
                        patch("/api/users/me/profile")
                                .with(
                                        authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        principal, null, List.of())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {
                      "nickname": "ogu",
                      "jobType": "BACKEND",
                      "careerLevel": "5년차"
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유효하지 않은 요청입니다."));
    }
}
