package com.ddd.webbb.security;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRedisConfig.class)
class SecurityEndpointTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private JwtProvider jwtProvider;

    private static ResultMatcher notUnauthorized() {
        return result ->
                assertNotEquals(
                        401,
                        result.getResponse().getStatus(),
                        "Expected endpoint to be publicly accessible but got 401");
    }

    @Nested
    @DisplayName("공개 엔드포인트는 인증 없이 접근 가능")
    class PublicEndpoints {

        @Test
        @DisplayName("GET /api/posts → 401이 아님")
        void getPosts() throws Exception {
            mockMvc.perform(get("/api/posts")).andExpect(notUnauthorized());
        }

        @Test
        @DisplayName("GET /api/posts/{postId} → 401이 아님")
        void getPostById() throws Exception {
            mockMvc.perform(get("/api/posts/1")).andExpect(notUnauthorized());
        }

        @Test
        @DisplayName("GET /api/users/{id} → 401이 아님")
        void getUserById() throws Exception {
            mockMvc.perform(get("/api/users/123")).andExpect(notUnauthorized());
        }

        @Test
        @DisplayName("POST /api/auth/signup/email → 401이 아님")
        void signupEmail() throws Exception {
            mockMvc.perform(
                            post("/api/auth/signup/email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                    .andExpect(notUnauthorized());
        }

        @Test
        @DisplayName("POST /api/auth/login/email → 401이 아님")
        void loginEmail() throws Exception {
            mockMvc.perform(
                            post("/api/auth/login/email")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                    .andExpect(notUnauthorized());
        }

        @Test
        @DisplayName("POST /api/auth/oauth/exchange → 401이 아님")
        void oauthExchange() throws Exception {
            mockMvc.perform(
                            post("/api/auth/oauth/exchange")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"code\": \"test\"}"))
                    .andExpect(notUnauthorized());
        }

        @Test
        @DisplayName("GET /actuator/health → 401이 아님")
        void actuatorHealth() throws Exception {
            mockMvc.perform(get("/actuator/health")).andExpect(notUnauthorized());
        }
    }

    @Nested
    @DisplayName("보호 엔드포인트는 인증 없이 401")
    class ProtectedEndpoints {

        @Test
        @DisplayName("GET /api/users/me → 401 + JSON 응답")
        void getUsersMe_without_token() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
        }

        @Test
        @DisplayName("POST /api/posts → 401")
        void createPost_without_token() throws Exception {
            mockMvc.perform(
                            post("/api/posts")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/auth/logout → 401")
        void logout_without_token() throws Exception {
            mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /api/auth/link/google → 401")
        void linkOAuth_without_token() throws Exception {
            mockMvc.perform(
                            post("/api/auth/link/google")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DELETE /api/auth/link/google → 401")
        void unlinkOAuth_without_token() throws Exception {
            mockMvc.perform(delete("/api/auth/link/google")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("/api/users/me vs /api/users/{id} 우선순위")
    class UsersMePriority {

        @Test
        @DisplayName("GET /api/users/me (토큰 없음) → 401, /api/users/{id} wildcard에 안 걸림")
        void usersMe_not_matched_by_wildcard() throws Exception {
            mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/users/me (토큰 있음) → 401이 아님")
        void usersMe_with_valid_token() throws Exception {
            String token = jwtProvider.createAccessToken(UUID.randomUUID());
            mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                    .andExpect(notUnauthorized());
        }

        @Test
        @DisplayName("GET /api/users/123 (토큰 없음) → 401이 아님 (public)")
        void usersById_without_token() throws Exception {
            mockMvc.perform(get("/api/users/123")).andExpect(notUnauthorized());
        }
    }
}
