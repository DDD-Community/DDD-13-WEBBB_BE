package com.ddd.webbb.global.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    POST_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요 기록이 존재하지 않습니다."),
    COMMENT_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "공감 기록이 존재하지 않습니다."),
    MONSTER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 몬스터입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    ALREADY_LIKED_POST(HttpStatus.CONFLICT, "이미 좋아요한 게시글입니다."),
    ALREADY_LIKED_COMMENT(HttpStatus.CONFLICT, "이미 공감한 댓글입니다."),
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    EMAIL_ALREADY_EXISTS_LINK_REQUIRED(
            HttpStatus.CONFLICT, "이미 해당 이메일로 가입된 계정이 있습니다. 기존 계정으로 로그인 후 계정 연동을 진행해주세요."),
    INVALID_PARENT_COMMENT(HttpStatus.BAD_REQUEST, "부모 댓글이 해당 게시글에 속하지 않거나 대댓글에는 답글을 달 수 없습니다."),
    COMMENT_POST_MISMATCH(HttpStatus.BAD_REQUEST, "해당 댓글은 이 게시글에 속하지 않습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    DUPLICATED_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    ALREADY_WITHDRAWN_USER(HttpStatus.BAD_REQUEST, "이미 탈퇴한 회원입니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth 제공자입니다."),
    OAUTH_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "OAuth 인증에 실패했습니다."),
    OAUTH_PROVIDER_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 해당 OAuth 제공자가 연동되어 있습니다."),
    OAUTH_ALREADY_LINKED(HttpStatus.CONFLICT, "해당 OAuth 계정은 다른 사용자에게 이미 연동되어 있습니다."),
    OAUTH_PROVIDER_NOT_LINKED(HttpStatus.NOT_FOUND, "해당 OAuth 제공자가 연동되어 있지 않습니다."),
    CANNOT_UNLINK_LAST_AUTH(HttpStatus.BAD_REQUEST, "마지막 인증 수단은 해제할 수 없습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
