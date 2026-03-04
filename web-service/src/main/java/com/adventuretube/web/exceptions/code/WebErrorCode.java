package com.adventuretube.web.exceptions.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum WebErrorCode {
    DATA_NOT_FOUND("Data not found", HttpStatus.NOT_FOUND),
    DUPLICATE_KEY("Duplicate entry already exists", HttpStatus.CONFLICT),
    SERVER_NOT_AVAILABLE("Geospatial service is not available", HttpStatus.SERVICE_UNAVAILABLE),
    SERVICE_CIRCUIT_OPEN("Geospatial service is temporarily unavailable, please try again later", HttpStatus.SERVICE_UNAVAILABLE),
    INTERNAL_ERROR("Unknown error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus httpStatus;

    WebErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
