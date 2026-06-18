package com.ddd.webbb.auth.interfaces.dto;

import com.ddd.webbb.user.domain.CareerLevel;
import com.ddd.webbb.user.domain.JobType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailSignupRequest(
        @Schema(example = "5959@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "이메일은 필수입니다.")
                @Email(message = "이메일 형식이 올바르지 않습니다.")
                String email,
        @Schema(example = "5959ohguohgu", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotBlank(message = "비밀번호는 필수입니다.")
                @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
                @Pattern(
                        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                        message = "비밀번호는 영문과 숫자를 포함해야 합니다.")
                String password,
        @Schema(
                        description = "선택 입력. 미전달 시 null로 저장되며, 후속 프로필 설정에서 입력할 수 있습니다.",
                        example = "ogu",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
                String nickname,
        @Schema(
                        description =
                                "선택 입력. 직군. PLANNING=기획, DESIGN=디자인, DEVELOPMENT=개발, MARKETING=마케팅, "
                                        + "SALES=영업, HR=인사, GENERAL_AFFAIRS=총무, PRODUCTION=생산, "
                                        + "ACCOUNTING=회계, OTHER=기타",
                        example = "DEVELOPMENT",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        allowableValues = {
                            "PLANNING",
                            "DESIGN",
                            "DEVELOPMENT",
                            "MARKETING",
                            "SALES",
                            "HR",
                            "GENERAL_AFFAIRS",
                            "PRODUCTION",
                            "ACCOUNTING",
                            "OTHER"
                        })
                JobType jobRole,
        @Schema(
                        description =
                                "선택 입력. 경력. NEWCOMER=신입, YEAR_1=1년차, YEAR_2=2년차, YEAR_3=3년차, "
                                        + "YEAR_4=4년차, YEAR_5=5년차, YEAR_6=6년차, YEAR_7_PLUS=7년차 이상",
                        example = "NEWCOMER",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        allowableValues = {
                            "NEWCOMER",
                            "YEAR_1",
                            "YEAR_2",
                            "YEAR_3",
                            "YEAR_4",
                            "YEAR_5",
                            "YEAR_6",
                            "YEAR_7_PLUS"
                        })
                CareerLevel careerYear) {}
