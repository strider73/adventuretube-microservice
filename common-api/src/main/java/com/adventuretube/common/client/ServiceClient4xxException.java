package com.adventuretube.common.client;

/**
 * Exception for 4xx client errors from inter-service calls.
 *
 * These are expected business errors (e.g., USER_NOT_FOUND, TOKEN_NOT_FOUND)
 * and should NOT trip the circuit breaker. Only 5xx server errors,
 * timeouts, and network failures should affect circuit breaker state.
 */
public class ServiceClient4xxException extends ServiceClientException {

    public ServiceClient4xxException(String serviceName, String errorCode, String message, int httpStatus) {
        super(serviceName, errorCode, message, httpStatus);
    }
}
