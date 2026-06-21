package com.ddd.webbb.post.domain;

import com.ddd.webbb.global.common.exception.AppException;
import com.ddd.webbb.global.common.exception.ErrorCode;
import java.util.Locale;

public enum PostOrder {
    LATEST,
    POPULAR;

    public static PostOrder from(String value) {
        if (value == null || value.isBlank()) {
            return LATEST;
        }

        try {
            return PostOrder.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
    }
}
