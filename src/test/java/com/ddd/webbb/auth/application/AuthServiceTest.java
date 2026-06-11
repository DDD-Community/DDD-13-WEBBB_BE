package com.ddd.webbb.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ddd.webbb.auth.infrastructure.RefreshTokenStore;
import com.ddd.webbb.auth.interfaces.dto.AuthResponse;
import com.ddd.webbb.auth.interfaces.dto.AuthResponse.TokenInfo;
import com.ddd.webbb.auth.interfaces.dto.EmailLoginRequest;
import com.ddd.webbb.auth.interfaces.dto.EmailSignupRequest;
import com.ddd.webbb.config.TestRedisConfig;
import com.ddd.webbb.global.auth.JwtProvider;
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.user.domain.CareerLevel;
import com.ddd.webbb.user.domain.JobType;
import com.ddd.webbb.user.domain.User;
import com.ddd.webbb.user.domain.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestRedisConfig.class)
@Transactional
class AuthServiceTest {

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtProvider jwtProvider;
    @MockitoBean private RefreshTokenStore refreshTokenStore;

    @Nested
    @DisplayName("이메일 회원가입")
    class SignupEmail {

        @Test
        @DisplayName("정상 회원가입 → 사용자 생성 + 토큰 발급")
        void success() {
            // Given
            EmailSignupRequest request =
                    new EmailSignupRequest(
                            "new@test.com",
                            "password123!",
                            "테스터",
                            JobType.DEVELOPMENT,
                            CareerLevel.NEWCOMER);

            // When
            AuthResponse response = authService.signupEmail(request);

            // Then
            assertThat(response.isNewUser()).isTrue();
            assertThat(response.user().email()).isEqualTo("new@test.com");
            assertThat(response.user().nickname()).isEqualTo("테스터");
            assertThat(response.user().jobRole()).isEqualTo("DEVELOPMENT");
            assertThat(response.user().careerYear()).isEqualTo("NEWCOMER");
            assertThat(response.tokens().accessToken()).isNotBlank();
            assertThat(response.tokens().refreshToken()).isNotBlank();
            assertThat(userRepository.existsByEmailAndDeletedAtIsNull("new@test.com")).isTrue();

            User savedUser =
                    userRepository.findByEmailAndDeletedAtIsNull("new@test.com").orElseThrow();
            assertThat(savedUser.getJobType()).isEqualTo("DEVELOPMENT");
            assertThat(savedUser.getCareerLevel()).isEqualTo("NEWCOMER");
        }

        @Test
        @DisplayName("비밀번호가 BCrypt로 해시되어 저장된다")
        void passwordHashed() {
            // Given
            EmailSignupRequest request =
                    new EmailSignupRequest("hash@test.com", "plain123!", "해시유저", null, null);

            // When
            authService.signupEmail(request);

            // Then
            User user = userRepository.findByEmailAndDeletedAtIsNull("hash@test.com").orElseThrow();
            assertThat(user.getPasswordHash()).isNotEqualTo("plain123!");
            assertThat(user.getPasswordHash()).startsWith("$2a$");
        }

        @Test
        @DisplayName("이미 존재하는 이메일 → DUPLICATED_EMAIL 예외")
        void duplicatedEmail() {
            // Given
            createUser("existing@test.com", "기존유저");
            EmailSignupRequest request =
                    new EmailSignupRequest("existing@test.com", "password123!", "새유저", null, null);

            // When / Then
            assertThatThrownBy(() -> authService.signupEmail(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.DUPLICATED_EMAIL));
        }

        @Test
        @DisplayName("이미 존재하는 닉네임 → DUPLICATED_NICKNAME 예외")
        void duplicatedNickname() {
            // Given
            createUser("other@test.com", "중복닉네임");
            EmailSignupRequest request =
                    new EmailSignupRequest("new@test.com", "password123!", "중복닉네임", null, null);

            // When / Then
            assertThatThrownBy(() -> authService.signupEmail(request))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.DUPLICATED_NICKNAME));
        }
    }

    @Nested
    @DisplayName("이메일 로그인")
    class LoginEmail {

        @Test
        @DisplayName("정상 로그인 → isNewUser=false + 토큰 발급")
        void success() {
            // Given
            authService.signupEmail(
                    new EmailSignupRequest("login@test.com", "password123!", "로그인유저", null, null));

            // When
            AuthResponse response =
                    authService.loginEmail(new EmailLoginRequest("login@test.com", "password123!"));

            // Then
            assertThat(response.isNewUser()).isFalse();
            assertThat(response.user().email()).isEqualTo("login@test.com");
            assertThat(response.tokens().accessToken()).isNotBlank();
            assertThat(response.tokens().refreshToken()).isNotBlank();
        }

        @Test
        @DisplayName("존재하지 않는 이메일 → USER_NOT_FOUND 예외")
        void userNotFound() {
            // When / Then
            assertThatThrownBy(
                            () ->
                                    authService.loginEmail(
                                            new EmailLoginRequest(
                                                    "nobody@test.com", "password123!")))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("잘못된 비밀번호 → INVALID_PASSWORD 예외")
        void invalidPassword() {
            // Given
            authService.signupEmail(
                    new EmailSignupRequest("pw@test.com", "correct123!", "비번유저", null, null));

            // When / Then
            assertThatThrownBy(
                            () ->
                                    authService.loginEmail(
                                            new EmailLoginRequest("pw@test.com", "wrong123!")))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.INVALID_PASSWORD));
        }

        @Test
        @DisplayName("탈퇴한 회원 로그인 → USER_NOT_FOUND 예외 (deletedAt 필터)")
        void withdrawnUser() {
            // Given
            authService.signupEmail(
                    new EmailSignupRequest(
                            "withdrawn@test.com", "password123!", "탈퇴유저", null, null));
            User user =
                    userRepository
                            .findByEmailAndDeletedAtIsNull("withdrawn@test.com")
                            .orElseThrow();
            user.withdraw();
            userRepository.saveAndFlush(user);

            // When / Then
            assertThatThrownBy(
                            () ->
                                    authService.loginEmail(
                                            new EmailLoginRequest(
                                                    "withdrawn@test.com", "password123!")))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Refresh {

        @Test
        @DisplayName("유효한 리프레시 토큰 → 새 토큰 쌍 발급")
        void success() {
            // Given
            UUID publicId = UUID.randomUUID();
            String refreshToken = jwtProvider.createRefreshToken(publicId);
            when(refreshTokenStore.find(publicId)).thenReturn(refreshToken);

            // When
            TokenInfo tokenInfo = authService.refresh(refreshToken);

            // Then
            assertThat(tokenInfo.accessToken()).isNotBlank();
            assertThat(tokenInfo.refreshToken()).isNotBlank();
            verify(refreshTokenStore).save(any(UUID.class), any(String.class));
        }

        @Test
        @DisplayName("유효하지 않은 리프레시 토큰 → INVALID_TOKEN 예외")
        void invalidToken() {
            // When / Then
            assertThatThrownBy(() -> authService.refresh("invalid-token"))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.INVALID_TOKEN));
        }

        @Test
        @DisplayName("저장소에 없는 리프레시 토큰 → INVALID_TOKEN 예외")
        void tokenNotInStore() {
            // Given
            UUID publicId = UUID.randomUUID();
            String refreshToken = jwtProvider.createRefreshToken(publicId);
            when(refreshTokenStore.find(publicId)).thenReturn(null);

            // When / Then
            assertThatThrownBy(() -> authService.refresh(refreshToken))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.INVALID_TOKEN));
        }

        @Test
        @DisplayName("저장소의 토큰과 불일치 → INVALID_TOKEN 예외")
        void tokenMismatch() {
            // Given
            UUID publicId = UUID.randomUUID();
            String refreshToken = jwtProvider.createRefreshToken(publicId);
            when(refreshTokenStore.find(publicId)).thenReturn("different-stored-token");

            // When / Then
            assertThatThrownBy(() -> authService.refresh(refreshToken))
                    .isInstanceOf(AppException.class)
                    .satisfies(
                            e ->
                                    assertThat(((AppException) e).getErrorCode())
                                            .isEqualTo(ErrorCode.INVALID_TOKEN));
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("정상 로그아웃 → 리프레시 토큰 삭제")
        void success() {
            // Given
            UUID publicId = UUID.randomUUID();

            // When
            authService.logout(publicId);

            // Then
            verify(refreshTokenStore).delete(publicId);
        }
    }

    private User createUser(String email, String nickname) {
        User user = User.create(email, nickname);
        return userRepository.saveAndFlush(user);
    }
}
