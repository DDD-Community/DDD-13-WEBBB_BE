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
import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import com.ddd.webbb.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "로그인/회원가입 성공",
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
                              "email": "ogu@gmail.com",
                              "nickname": "ogu",
                              "jobRole": "DEVELOPMENT",
                              "careerYear": "YEAR_3",
                              "status": "ACTIVE"
                            },
                            "tokens": {
                              "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                              "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                            },
                            "isNewUser": false
                          },
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "OAuth 인증 실패",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "OAuth 인증에 실패했습니다.",
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """)))
    })
    @PostMapping("/oauth/{provider}")
    public ApiResponse<AuthResponse> oauthLogin(
            @Parameter(description = "GOOGLE | KAKAO | NAVER") @PathVariable String provider,
            @RequestBody @Valid OAuthLoginRequest request) {
        AuthResponse response = authService.oauthLogin(provider, request);
        return ApiResponse.ok("로그인에 성공했습니다.", response);
    }

    @Operation(summary = "OAuth 코드 → 토큰 교환")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "토큰 교환 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "토큰 교환에 성공했습니다.",
                          "data": {
                            "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                            "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                          },
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "유효하지 않은 코드",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "유효하지 않은 토큰입니다.",
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """)))
    })
    @PostMapping("/oauth/exchange")
    public ApiResponse<TokenInfo> oauthExchange(
            @RequestBody @Valid OAuthCodeExchangeRequest request) {
        OAuthCodeStore.TokenPair tokenPair = oAuthCodeStore.exchange(request.code());
        return ApiResponse.ok(
                "토큰 교환에 성공했습니다.", new TokenInfo(tokenPair.accessToken(), tokenPair.refreshToken()));
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
                            "user": {
                              "id": "01939b10-7b0f-7c8f-9a2b-111111111111",
                              "email": "ogu@example.com",
                              "nickname": "ogu",
                              "jobRole": "DEVELOPMENT",
                              "careerYear": "NEWCOMER",
                              "status": "ACTIVE"
                            },
                            "tokens": {
                              "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                              "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                            },
                            "isNewUser": true
                          },
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "이메일 또는 닉네임 중복",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "이미 사용 중인 이메일입니다.",
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "유효성 검증 실패",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "유효하지 않은 요청입니다.",
                          "errors": [
                            { "field": "email", "reason": "must not be blank" },
                            { "field": "password", "reason": "must not be blank" }
                          ],
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """)))
    })
    @PostMapping("/signup/email")
    public ResponseEntity<ApiResponse<AuthResponse>> signupEmail(
            @RequestBody @Valid EmailSignupRequest request) {
        AuthResponse response = authService.signupEmail(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("회원가입에 성공했습니다.", response));
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
                            "user": {
                              "id": "01939b10-7b0f-7c8f-9a2b-111111111111",
                              "email": "ogu@example.com",
                              "nickname": "ogu",
                              "jobRole": "DEVELOPMENT",
                              "careerYear": "YEAR_3",
                              "status": "ACTIVE"
                            },
                            "tokens": {
                              "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                              "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                            },
                            "isNewUser": false
                          },
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "이메일 또는 비밀번호 불일치",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "이메일 또는 비밀번호가 올바르지 않습니다.",
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "존재하지 않는 회원",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "존재하지 않는 회원입니다.",
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """)))
    })
    @PostMapping("/login/email")
    public ApiResponse<AuthResponse> loginEmail(@RequestBody @Valid EmailLoginRequest request) {
        AuthResponse response = authService.loginEmail(request);
        return ApiResponse.ok("로그인에 성공했습니다.", response);
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
                          "data": null,
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "인증이 필요합니다.",
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """)))
    })
    @PostMapping("/logout")
    public ApiResponse<Void> logout(Principal principal) {
        UUID publicId = UUID.fromString(principal.getName());
        authService.logout(publicId);
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
                          "data": {
                            "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                            "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                          },
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "유효하지 않은 Refresh Token",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "유효하지 않은 토큰입니다.",
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """)))
    })
    @PostMapping("/refresh")
    public ApiResponse<TokenInfo> refresh(
            @RequestHeader("Authorization") String authorizationHeader) {
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
        String refreshToken = authorizationHeader.substring(7);
        TokenInfo tokenInfo = authService.refresh(refreshToken);
        return ApiResponse.ok("토큰이 재발급되었습니다.", tokenInfo);
    }

    @Operation(summary = "OAuth 계정 연동")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "계정 연동 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "OAuth 계정이 연동되었습니다.",
                          "data": null,
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "이미 연동된 OAuth 제공자",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "이미 해당 OAuth 제공자가 연동되어 있습니다.",
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """)))
    })
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
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "연동 해제 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "OAuth 계정 연동이 해제되었습니다.",
                          "data": null,
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "마지막 인증 수단 해제 불가",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "마지막 인증 수단은 해제할 수 없습니다.",
                          "timestamp": "2026-05-21T12:00:00"
                        }
                        """)))
    })
    @DeleteMapping("/link/{provider}")
    public ApiResponse<Void> unlinkOAuth(
            @Parameter(description = "GOOGLE | KAKAO | NAVER") @PathVariable String provider,
            Principal principal) {
        UUID publicId = UUID.fromString(principal.getName());
        authService.unlinkOAuth(publicId, provider);
        return ApiResponse.ok("OAuth 계정 연동이 해제되었습니다.", null);
    }
}
