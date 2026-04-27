package com.dnd.poc.retrospective.domain;

public sealed interface RetrospectiveResult
        permits RetrospectiveResult.Success, RetrospectiveResult.Failure {

    record Success(KptAnalysis analysis) implements RetrospectiveResult {}

    record Failure(ErrorCode code, String detail) implements RetrospectiveResult {}

    enum ErrorCode {
        UPSTREAM_TIMEOUT(504),
        UPSTREAM_UNAVAILABLE(502),
        EMPTY_RESPONSE(422),
        INVALID_RESPONSE(422),
        FALLBACK_TRIGGERED(200);

        private final int httpStatus;

        ErrorCode(int httpStatus) {
            this.httpStatus = httpStatus;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }
}
