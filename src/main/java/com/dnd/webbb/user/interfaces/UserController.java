package com.dnd.webbb.user.interfaces;

import com.dnd.webbb.global.common.response.ApiResponse;
import com.dnd.webbb.user.application.UserService;
import com.dnd.webbb.user.interfaces.dto.UserCreateRequest;
import com.dnd.webbb.user.interfaces.dto.UserListResponse;
import com.dnd.webbb.user.interfaces.dto.UserResponse;
import com.dnd.webbb.user.interfaces.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "회원 API")
@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "회원 생성")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "생성 성공",
                content =
                        @Content(
                                schema = @Schema(implementation = ApiResponse.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "회원이 생성되었습니다.",
                          "data": {
                            "id": "01939b10-7b0f-7c8f-9a2b-111111111111",
                            "email": "test@test.com",
                            "nickname": "ogu",
                            "status": "ACTIVE",
                            "createdAt": "2026-04-09T12:00:00"
                          },
                          "timestamp": "2026-04-09T12:00:00"
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
                            { "field": "email", "reason": "이메일 형식이 올바르지 않습니다." },
                            { "field": "nickname", "reason": "닉네임은 10자 이하여야 합니다." }
                          ],
                          "timestamp": "2026-04-09T12:00:00"
                        }
                        """)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @RequestBody @Valid UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("회원이 생성되었습니다.", userService.createUser(request)));
    }

    @Operation(summary = "회원 단건 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "요청이 성공했습니다.",
                          "data": {
                            "id": "01939b10-7b0f-7c8f-9a2b-111111111111",
                            "email": "test@test.com",
                            "nickname": "ogu",
                            "status": "ACTIVE",
                            "createdAt": "2026-04-09T12:00:00"
                          },
                          "timestamp": "2026-04-09T12:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "회원 없음",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "존재하지 않는 회원입니다.",
                          "timestamp": "2026-04-09T12:00:00"
                        }
                        """)))
    })
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable UUID id) {
        return ApiResponse.ok(userService.getUser(id));
    }

    @Operation(summary = "회원 목록 조회 (커서 기반 페이지네이션)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "요청이 성공했습니다.",
                          "data": {
                            "users": [
                              { "id": "01939b10-7b0f-7c8f-9a2b-222222222222", "email": "b@test.com", "nickname": "bbb", "status": "ACTIVE", "createdAt": "2026-04-09T11:00:00" },
                              { "id": "01939b10-7b0f-7c8f-9a2b-111111111111", "email": "a@test.com", "nickname": "aaa", "status": "ACTIVE", "createdAt": "2026-04-09T10:00:00" }
                            ],
                            "nextCursor": 19
                          },
                          "timestamp": "2026-04-09T12:00:00"
                        }
                        """)))
    })
    @GetMapping
    public ApiResponse<UserListResponse> getUsers(
            @Parameter(description = "마지막으로 받은 회원 id (첫 요청 시 생략)") @RequestParam(required = false)
                    Long cursor,
            @Parameter(description = "페이지 크기 (기본값: 20, 최대: 100)")
                    @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
                    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
                    int size) {
        return ApiResponse.ok(userService.getUsers(cursor, size));
    }

    @Operation(summary = "회원 정보 수정")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "수정 성공",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": true,
                          "message": "요청이 성공했습니다.",
                          "data": {
                            "id": "01939b10-7b0f-7c8f-9a2b-111111111111",
                            "email": "test@test.com",
                            "nickname": "newname",
                            "status": "ACTIVE",
                            "createdAt": "2026-04-09T12:00:00"
                          },
                          "timestamp": "2026-04-09T12:00:00"
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
                            { "field": "nickname", "reason": "닉네임은 10자 이하여야 합니다." }
                          ],
                          "timestamp": "2026-04-09T12:00:00"
                        }
                        """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "회원 없음",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "존재하지 않는 회원입니다.",
                          "timestamp": "2026-04-09T12:00:00"
                        }
                        """)))
    })
    @PatchMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable UUID id, @RequestBody @Valid UserUpdateRequest request) {
        return ApiResponse.ok(userService.updateUser(id, request));
    }

    @Operation(summary = "회원 탈퇴 (소프트 삭제)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "204",
                description = "탈퇴 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "회원 없음",
                content =
                        @Content(
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                        {
                          "success": false,
                          "message": "존재하지 않는 회원입니다.",
                          "timestamp": "2026-04-09T12:00:00"
                        }
                        """)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> withdrawUser(@PathVariable UUID id) {
        userService.withdrawUser(id);
        return ResponseEntity.noContent().build();
    }
}
