package com.ddd.webbb.auth.interfaces;

import com.ddd.webbb.auth.interfaces.dto.AuthResponse;
import com.ddd.webbb.auth.interfaces.dto.AuthResponse.TokenInfo;
import com.ddd.webbb.auth.interfaces.dto.AuthResponse.UserInfo;
import com.ddd.webbb.auth.interfaces.dto.EmailLoginRequest;
import com.ddd.webbb.auth.interfaces.dto.EmailSignupRequest;
import com.ddd.webbb.auth.interfaces.dto.OAuthLoginRequest;
import com.ddd.webbb.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final UserInfo STUB_USER =
            new UserInfo(
                    "01939b10-7b0f-7c8f-9a2b-111111111111",
                    "ogu@test.com",
                    "ogu",
                    "DEVELOPMENT",
                    "YEAR_3",
                    "ACTIVE");

    private static final TokenInfo STUB_TOKENS =
            new TokenInfo("stub-access-token", "stub-refresh-token");

    @Operation(summary = "SNS 로그인/회원가입")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "로그인 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "로그인에 성공했습니다.",
                          "data": {
                            "user": {
                              "id": "01939b10-7b0f-7c8f-9a2b-111111111111",
                              "email": "ogu@test.com",
                              "nickname": "ogu",
                              "jobRole": "DEVELOPMENT",
                              "careerYear": "YEAR_3",
                              "status": "ACTIVE"
                            },
                            "tokens": {
                              "accessToken": "stub-access-token",
                              "refreshToken": "stub-refresh-token"
                            },
                            "isNewUser": true
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @PostMapping("/oauth/{provider}")
    public ApiResponse<AuthResponse> oauthLogin(
            @Parameter(description = "GOOGLE | KAKAO | NAVER") @PathVariable String provider,
            @RequestBody @Valid OAuthLoginRequest request) {
        // TODO: 실제 서비스 연동
        return ApiResponse.ok("로그인에 성공했습니다.", new AuthResponse(STUB_USER, STUB_TOKENS, true));
    }

    @Operation(summary = "이메일 회원가입")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "회원가입 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "회원가입에 성공했습니다.",
                          "data": {
                            "user": { "id": "01939b10-7b0f-7c8f-9a2b-111111111111", "email": "test@test.com", "nickname": "ogu", "jobRole": "DEVELOPMENT", "careerYear": "NEWCOMER", "status": "ACTIVE" },
                            "tokens": { "accessToken": "stub-access-token", "refreshToken": "stub-refresh-token" },
                            "isNewUser": true
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @PostMapping("/signup/email")
    public ResponseEntity<ApiResponse<AuthResponse>> signupEmail(
            @RequestBody @Valid EmailSignupRequest request) {
        // TODO: 실제 서비스 연동
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                "회원가입에 성공했습니다.", new AuthResponse(STUB_USER, STUB_TOKENS, true)));
    }

    @Operation(summary = "이메일 로그인")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "로그인 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "로그인에 성공했습니다.",
                          "data": {
                            "user": { "id": "01939b10-7b0f-7c8f-9a2b-111111111111", "email": "test@test.com", "nickname": "ogu", "jobRole": "DEVELOPMENT", "careerYear": "YEAR_3", "status": "ACTIVE" },
                            "tokens": { "accessToken": "stub-access-token", "refreshToken": "stub-refresh-token" },
                            "isNewUser": false
                          },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @PostMapping("/login/email")
    public ApiResponse<AuthResponse> loginEmail(@RequestBody @Valid EmailLoginRequest request) {
        // TODO: 실제 서비스 연동
        return ApiResponse.ok("로그인에 성공했습니다.", new AuthResponse(STUB_USER, STUB_TOKENS, false));
    }

    @Operation(summary = "로그아웃")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "로그아웃 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "로그아웃에 성공했습니다.",
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // TODO: 실제 서비스 연동
        return ApiResponse.ok("로그아웃에 성공했습니다.", null);
    }

    @Operation(summary = "Access Token 재발급")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "토큰 재발급 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "토큰이 재발급되었습니다.",
                          "data": { "accessToken": "new-stub-access-token", "refreshToken": "new-stub-refresh-token" },
                          "timestamp": "2026-04-27T21:00:00"
                        }
                        """)))
    })
    @PostMapping("/refresh")
    public ApiResponse<TokenInfo> refresh() {
        // TODO: 실제 서비스 연동
        return ApiResponse.ok(
                "토큰이 재발급되었습니다.", new TokenInfo("new-stub-access-token", "new-stub-refresh-token"));
    }
}
