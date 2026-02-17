package com.adventuretube.member.exceptions;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.member.exceptions.base.BaseServiceException;
import com.adventuretube.member.exceptions.code.MemberErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Component
@Order(-2)
@AllArgsConstructor
public class GlobalWebExceptionHandler implements WebExceptionHandler {
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (ex instanceof DuplicateException duplicateEx) {
            return writeErrorResponse(exchange, duplicateEx.getErrorCode(),
                    duplicateEx.getOrigin() + " : member-service");
        }
        if (ex instanceof MemberNotFoundException notFoundEx) {
            return writeErrorResponse(exchange, notFoundEx.getErrorCode(),
                    notFoundEx.getOrigin() + " : member-service");
        }
        log.error("Unhandled exception", ex);
        return writeErrorResponse(exchange, MemberErrorCode.UNKNOWN_EXCEPTION, "member-service");
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, MemberErrorCode code, String origin) {
        ServiceResponse<?> response = ServiceResponse.builder()
                .success(false)
                .message(code.getMessage())
                .errorCode(code.name())
                .data(origin)
                .timestamp(LocalDateTime.now())
                .build();

        exchange.getResponse().setStatusCode(code.getHttpStatus());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(response);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response", e);
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }
}
