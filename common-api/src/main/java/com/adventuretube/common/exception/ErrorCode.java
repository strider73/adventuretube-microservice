package com.adventuretube.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Contract implemented by every per-service error-code enum
 * (AuthErrorCode, MemberErrorCode, GeoErrorCode, WebErrorCode).
 *
 * <p>Each service owns its own error-code vocabulary, but they all share
 * this contract so common infrastructure (BaseServiceException, future
 * shared exception handlers) can read message and HTTP status from any
 * service's error code without depending on the concrete enum type.
 *
 * <p>Java enums automatically satisfy {@link #name()} via {@link Enum#name()},
 * so enum implementations only need to expose {@link #getMessage()} and
 * {@link #getHttpStatus()} (typically via Lombok's {@code @Getter}).
 */
public interface ErrorCode {
    String getMessage();
    HttpStatus getHttpStatus();
    String name();
}