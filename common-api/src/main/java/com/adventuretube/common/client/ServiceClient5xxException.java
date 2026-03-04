package com.adventuretube.common.client;

/**
 * Exception for 5xx server errors from inter-service calls.
 *
 * These indicate infrastructure failures in the downstream service
 * and SHOULD trip the circuit breaker.
 */
public class ServiceClient5xxException extends ServiceClientException {

    public ServiceClient5xxException(String serviceName, String errorCode, String message, int httpStatus) {
        super(serviceName, errorCode, message, httpStatus);
    }
}
