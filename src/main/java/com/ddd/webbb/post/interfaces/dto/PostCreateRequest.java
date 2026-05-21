package com.ddd.webbb.post.interfaces.dto;

import com.ddd.webbb.post.domain.CommentTone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @Schema(
                        description = "작성할 고민글 본문. 최대 500자까지 입력할 수 있습니다.",
                        example = "면접에서 계속 떨어져서 점점 자신감이 사라져요.")
                @NotBlank(message = "게시글 내용은 필수입니다.")
                @Size(max = 500, message = "게시글은 500자 이하여야 합니다.")
                String content,
        @Schema(
                        description =
                                "원하는 댓글 톤. "
                                        + "VENT_WITH_ME=대신 욕해주기, "
                                        + "COMFORT_ME=무조건 위로해주기, "
                                        + "WARM_ADVICE=따뜻한 조언해주기, "
                                        + "MAKE_ME_LAUGH=웃겨주기",
                        example = "COMFORT_ME",
                        allowableValues = {
                            "VENT_WITH_ME",
                            "COMFORT_ME",
                            "WARM_ADVICE",
                            "MAKE_ME_LAUGH"
                        })
                @NotNull(message = "댓글 톤은 필수입니다.")
                CommentTone commentTone) {}
