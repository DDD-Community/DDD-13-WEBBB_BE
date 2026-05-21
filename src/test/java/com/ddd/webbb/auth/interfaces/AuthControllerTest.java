package com.ddd.webbb.auth.interfaces;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddd.webbb.auth.infrastructure.RefreshTokenStore;
import com.ddd.webbb.config.TestRedisConfig;
import com.ddd.webbb.global.auth.JwtProvider;
import java.util.UUID;
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
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtProvider jwtProvider;
    @MockitoBean private RefreshTokenStore refreshTokenStore;

    @Nested
    @DisplayName("POST /api/auth/signup/email")
    class SignupEmail {

        @Test
        @DisplayName("정상 회원가입 → 201 + 사용자/토큰 반환")
        void success() throws Exception {
            mockMvc.perform(
                            post("/api/auth/signup/email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {
                        "email": "new@test.com",
                        "password": "password123!",
                        "nickname": "테스터",
                        "jobRole": "DEVELOPMENT",
                        "careerYear": "NEWCOMER"
                    }
                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.user.email").value("new@test.com"))
                    .andExpect(jsonPath("$.data.user.nickname").value("테스터"))
                    .andExpect(jsonPath("$.data.tokens.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.tokens.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.isNewUser").value(true));
        }

        @Test
        @DisplayName("이메일 중복 → 409")
        void duplicatedEmail() throws Exception {
            // Given
            signup("dup@test.com", "기존유저");

            // When / Then
            mockMvc.perform(
                            post("/api/auth/signup/email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {
                        "email": "dup@test.com",
                        "password": "password123!",
                        "nickname": "새유저"
                    }
                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("필수 필드 누락 → 400")
        void missingFields() throws Exception {
            mockMvc.perform(
                            post("/api/auth/signup/email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {"email": "test@test.com"}
                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login/email")
    class LoginEmail {

        @Test
        @DisplayName("정상 로그인 → 200 + 토큰 반환")
        void success() throws Exception {
            // Given
            signup("login@test.com", "로그인유저");

            // When / Then
            mockMvc.perform(
                            post("/api/auth/login/email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {
                        "email": "login@test.com",
                        "password": "password123!"
                    }
                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.user.email").value("login@test.com"))
                    .andExpect(jsonPath("$.data.isNewUser").value(false))
                    .andExpect(jsonPath("$.data.tokens.accessToken").isNotEmpty());
        }

        @Test
        @DisplayName("잘못된 비밀번호 → 401")
        void invalidPassword() throws Exception {
            // Given
            signup("pw@test.com", "비번유저");

            // When / Then
            mockMvc.perform(
                            post("/api/auth/login/email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {
                        "email": "pw@test.com",
                        "password": "wrong123!"
                    }
                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("존재하지 않는 이메일 → 404")
        void userNotFound() throws Exception {
            mockMvc.perform(
                            post("/api/auth/login/email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            """
                    {
                        "email": "nobody@test.com",
                        "password": "password123!"
                    }
                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshApi {

        @Test
        @DisplayName("유효한 리프레시 토큰 → 200 + 새 토큰 반환")
        void success() throws Exception {
            // Given
            UUID publicId = UUID.randomUUID();
            String refreshToken = jwtProvider.createRefreshToken(publicId);
            when(refreshTokenStore.find(publicId)).thenReturn(refreshToken);

            // When / Then
            mockMvc.perform(
                            post("/api/auth/refresh")
                                    .header("Authorization", "Bearer " + refreshToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰 → 401")
        void invalidToken() throws Exception {
            mockMvc.perform(
                            post("/api/auth/refresh")
                                    .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutApi {

        @Test
        @DisplayName("토큰 없이 로그아웃 → 401")
        void withoutToken() throws Exception {
            mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
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
    }
}
