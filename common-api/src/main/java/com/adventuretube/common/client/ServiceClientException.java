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

    @Override
    public String toString() {
        return String.format("ServiceClientException{service='%s', errorCode='%s', httpStatus=%d, message='%s'}",
                serviceName, errorCode, httpStatus, getMessage());
    }
}
