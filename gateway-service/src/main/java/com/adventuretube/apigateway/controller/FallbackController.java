package com.adventuretube.apigateway.controller;

import com.adventuretube.common.api.response.ServiceResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Fallback controller for gateway circuit breaker (Layer 1).
 * Invoked via forward:/fallback/{serviceName} when a downstream service
 * is unreachable or the circuit breaker is open.
 */
@Slf4j
@RestController
public class FallbackController {

    @RequestMapping("/fallback/{serviceName}")
    public Mono<ResponseEntity<ServiceResponse<?>>> fallback(
            @PathVariable String serviceName,
            ServerWebExchange exchange) {

        Throwable exception = exchange.getAttribute(
                ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);

        String errorCode;
        String message;

        if (exception instanceof CallNotPermittedException) {
            errorCode = "SERVICE_CIRCUIT_OPEN";
            message = serviceName + " circuit breaker is open, service is temporarily unavailable";
            log.warn("Circuit breaker OPEN for {}", serviceName);
        } else {
            errorCode = "SERVICE_UNAVAILABLE";
            message = serviceName + " is temporarily unavailable, please try again later";
            log.error("Gateway fallback for {}", serviceName, exception);
        }

        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
