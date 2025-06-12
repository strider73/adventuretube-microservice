package com.adventuretube.geospatial.exceptions.code;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GeoErrorCode {
    ENV_FILE_NOT_FOUND("ENV_FILE_NOT_FOUND", HttpStatus.INTERNAL_SERVER_ERROR),

    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),

    //UNKNOWN EXCEPTION
    UNKNOWN_EXCEPTION("Unknown error", HttpStatus.INTERNAL_SERVER_ERROR);
    private final String message;
    private final HttpStatus httpStatus;

    GeoErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
