package com.adventuretube.common.client;

import lombok.Getter;

/**
 * Exception thrown when an inter-service call fails.
 *
 * Contains:
 * - serviceName: Which service failed (e.g., "MEMBER-SERVICE")
 * - errorCode: Error code from the remote service response
 * - message: Error message
 * - httpStatus: HTTP status code
 *
 * This allows callers to handle specific error cases appropriately.
 */
@Getter
public class ServiceClientException extends RuntimeException {

    private final String serviceName;
    private final String errorCode;
    private final int httpStatus;

    public ServiceClientException(String serviceName, String errorCode, String message, int httpStatus) {
        super(message);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * Cause-preserving constructor. Use this when wrapping an upstream exception
     * (e.g. WebClientRequestException, TimeoutException, or another ServiceClientException)
     * so the original failure is reachable via {@link #getCause()} and shows up
     * as a "Caused by:" line in stack traces and log entries.
     */
    public ServiceClientException(String serviceName, String errorCode, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public boolean isClientError() { return httpStatus >= 400 && httpStatus < 500; }
    public boolean isServerError() { return httpStatus >= 500; }

    @Override
    public String toString() {
        return String.format("ServiceClientException{service='%s', errorCode='%s', httpStatus=%d, message='%s'}",
                serviceName, errorCode, httpStatus, getMessage());
    }
}
