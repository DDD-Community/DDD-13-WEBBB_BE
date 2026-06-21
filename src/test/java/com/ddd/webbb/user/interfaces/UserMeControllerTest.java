package com.ddd.webbb.user.interfaces;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.ddd.webbb.user.domain.User;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
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
class UserMeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserService userService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockitoBean private OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    @MockitoBean private RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Test
    @DisplayName("GET /api/users/me 는 인증 사용자의 정보를 반환한다")
    void getMe_returnsAuthenticatedUser() throws Exception {
        UUID publicId = UUID.randomUUID();
        CustomUserPrincipal principal = new CustomUserPrincipal(publicId, "ogu@test.com", "ogu");
        User user = User.createOAuthUser("ogu@test.com", "ogu", "DEVELOPMENT", "YEAR_3");

        given(userService.getUserEntity(eq(publicId))).willReturn(user);

        mockMvc.perform(
                        get("/api/users/me")
                                .with(
                                        authentication(
                                                new UsernamePasswordAuthenticationToken(
                                                        principal, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(user.getPublicId().toString()))
                .andExpect(jsonPath("$.data.email").value("ogu@test.com"))
                .andExpect(jsonPath("$.data.nickname").value("ogu"))
                .andExpect(jsonPath("$.data.jobType").value("DEVELOPMENT"))
                .andExpect(jsonPath("$.data.careerLevel").value("YEAR_3"));
    }

    @Test
    @DisplayName("PATCH /api/users/me/profile 는 인증 사용자의 프로필을 수정한다")
    void updateMyProfile_updatesAuthenticatedUser() throws Exception {
        UUID publicId = UUID.randomUUID();
        CustomUserPrincipal principal = new CustomUserPrincipal(publicId, "ogu@test.com", "ogu");
        User updatedUser = User.createOAuthUser("ogu@test.com", "newogu", "PLANNING", "YEAR_5");

        given(userService.updateProfile(eq(publicId), eq("newogu"), eq("PLANNING"), eq("YEAR_5")))
                .willReturn(updatedUser);

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
                      "nickname": "newogu",
                      "jobType": "PLANNING",
                      "careerLevel": "YEAR_5"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("ogu@test.com"))
                .andExpect(jsonPath("$.data.nickname").value("newogu"))
                .andExpect(jsonPath("$.data.jobType").value("PLANNING"))
                .andExpect(jsonPath("$.data.careerLevel").value("YEAR_5"));
    }
}
