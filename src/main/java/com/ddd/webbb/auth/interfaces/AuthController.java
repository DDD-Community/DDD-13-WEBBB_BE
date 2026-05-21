package com.ddd.webbb.auth.interfaces;

import com.ddd.webbb.auth.application.AuthService;
import com.ddd.webbb.auth.infrastructure.OAuthCodeStore;
import com.ddd.webbb.auth.interfaces.dto.AuthResponse;
import com.ddd.webbb.auth.interfaces.dto.AuthResponse.TokenInfo;
import com.ddd.webbb.auth.interfaces.dto.EmailLoginRequest;
import com.ddd.webbb.auth.interfaces.dto.EmailSignupRequest;
import com.ddd.webbb.auth.interfaces.dto.OAuthCodeExchangeRequest;
import com.ddd.webbb.auth.interfaces.dto.OAuthLinkRequest;
import com.ddd.webbb.auth.interfaces.dto.OAuthLoginRequest;
import com.ddd.webbb.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthCodeStore oAuthCodeStore;

    public AuthController(AuthService authService, OAuthCodeStore oAuthCodeStore) {
        this.authService = authService;
        this.oAuthCodeStore = oAuthCodeStore;
    }

    @Operation(summary = "SNS 로그인/회원가입")
    @PostMapping("/oauth/{provider}")
    public ApiResponse<AuthResponse> oauthLogin(
            @Parameter(description = "GOOGLE | KAKAO | NAVER") @PathVariable String provider,
            @RequestBody @Valid OAuthLoginRequest request) {
        AuthResponse response = authService.oauthLogin(provider, request);
        return ApiResponse.ok("로그인에 성공했습니다.", response);
    }

    @Operation(summary = "OAuth 코드 → 토큰 교환")
    @PostMapping("/oauth/exchange")
    public ApiResponse<TokenInfo> oauthExchange(
            @RequestBody @Valid OAuthCodeExchangeRequest request) {
        OAuthCodeStore.TokenPair tokenPair = oAuthCodeStore.exchange(request.code());
        return ApiResponse.ok(
                "토큰 교환에 성공했습니다.", new TokenInfo(tokenPair.accessToken(), tokenPair.refreshToken()));
    }

    @Operation(summary = "이메일 회원가입")
    @PostMapping("/signup/email")
    public ResponseEntity<ApiResponse<AuthResponse>> signupEmail(
            @RequestBody @Valid EmailSignupRequest request) {
        AuthResponse response = authService.signupEmail(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("회원가입에 성공했습니다.", response));
    }

    @Operation(summary = "이메일 로그인")
    @PostMapping("/login/email")
    public ApiResponse<AuthResponse> loginEmail(@RequestBody @Valid EmailLoginRequest request) {
        AuthResponse response = authService.loginEmail(request);
        return ApiResponse.ok("로그인에 성공했습니다.", response);
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(Principal principal) {
        UUID publicId = UUID.fromString(principal.getName());
        authService.logout(publicId);
        return ApiResponse.ok("로그아웃에 성공했습니다.", null);
    }

    @Operation(summary = "Access Token 재발급")
    @PostMapping("/refresh")
    public ApiResponse<TokenInfo> refresh(
            @RequestHeader("Authorization") String authorizationHeader) {
        String refreshToken = authorizationHeader.replace("Bearer ", "");
        TokenInfo tokenInfo = authService.refresh(refreshToken);
        return ApiResponse.ok("토큰이 재발급되었습니다.", tokenInfo);
    }

    @Operation(summary = "OAuth 계정 연동")
    @PostMapping("/link/{provider}")
    public ApiResponse<Void> linkOAuth(
            @Parameter(description = "GOOGLE | KAKAO | NAVER") @PathVariable String provider,
            @RequestBody @Valid OAuthLinkRequest request,
            Principal principal) {
        UUID publicId = UUID.fromString(principal.getName());
        authService.linkOAuth(publicId, provider, request.oauthAccessToken());
        return ApiResponse.ok("OAuth 계정이 연동되었습니다.", null);
    }

    @Operation(summary = "OAuth 계정 연동 해제")
    @DeleteMapping("/link/{provider}")
    public ApiResponse<Void> unlinkOAuth(
            @Parameter(description = "GOOGLE | KAKAO | NAVER") @PathVariable String provider,
            Principal principal) {
        UUID publicId = UUID.fromString(principal.getName());
        authService.unlinkOAuth(publicId, provider);
        return ApiResponse.ok("OAuth 계정 연동이 해제되었습니다.", null);
    }
}
