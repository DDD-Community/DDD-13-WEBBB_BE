package com.ddd.webbb.user.interfaces.dto;

import com.ddd.webbb.user.domain.CareerLevel;
import com.ddd.webbb.user.domain.JobType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Schema(example = "newogu") @Size(min = 1, max = 10, message = "닉네임은 1자 이상 10자 이하여야 합니다.")
                String nickname,
        @Schema(
                        description =
                                "직군. PLANNING=기획, DESIGN=디자인, DEVELOPMENT=개발, MARKETING=마케팅, "
                                        + "SALES=영업, HR=인사, GENERAL_AFFAIRS=총무, PRODUCTION=생산, "
                                        + "ACCOUNTING=회계, OTHER=기타",
                        example = "PLANNING",
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
                JobType jobType,
        @Schema(
                        description =
                                "경력. NEWCOMER=신입, YEAR_1=1년차, YEAR_2=2년차, YEAR_3=3년차, "
                                        + "YEAR_4=4년차, YEAR_5=5년차, YEAR_6=6년차, YEAR_7_PLUS=7년차 이상",
                        example = "YEAR_3",
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
                CareerLevel careerLevel) {}
