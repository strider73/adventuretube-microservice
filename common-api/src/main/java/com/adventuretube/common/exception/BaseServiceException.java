package com.adventuretube.common.exception;

/**
 * Common base for all per-service business exceptions in AdventureTube.
 *
 * <p>This class lives in {@code common-api} so every service shares one
 * implementation rather than maintaining four near-identical copies.
 *
 * <p>The {@code errorCode} field is typed as {@link ErrorCode} (the
 * interface), so any service's enum that {@code implements ErrorCode}
 * (AuthErrorCode, MemberErrorCode, GeoErrorCode, WebErrorCode) can be
 * passed in. Java forbids generic exception classes, so the type
 * parameter pattern ({@code <E extends Enum<E> & ErrorCode>}) cannot be
 * used here — the interface is the substitute.
 *
 * <p>The class auto-captures the call site (className.methodName from
 * the top of the stack trace) into the {@code origin} field for
 * diagnostic purposes — preserves the behavior of the four duplicate
 * BaseServiceException files this class replaces.
 */
public abstract class BaseServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String originMethod;

    protected BaseServiceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.originMethod = captureOrigin();
    }

    /**
     * Cause-preserving constructor. Use this when wrapping an upstream
     * exception so the original failure is reachable via {@link #getCause()}
     * and shows up as a "Caused by:" line in stack traces and log entries.
     */
    protected BaseServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.originMethod = captureOrigin();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getOriginMethod() {
        return originMethod;
    }

    private String captureOrigin() {
        StackTraceElement[] stackTrace = this.getStackTrace();
        return stackTrace.length > 0
                ? stackTrace[0].getClassName() + "." + stackTrace[0].getMethodName()
                : "UnknownOrigin";
    }
}
