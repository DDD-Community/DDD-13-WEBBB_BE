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
    }
}
