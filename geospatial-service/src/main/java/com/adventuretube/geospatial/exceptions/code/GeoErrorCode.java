package com.adventuretube.geospatial.exceptions.code;

import com.adventuretube.common.api.code.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum GeoErrorCode implements ErrorCode {
    ENV_FILE_NOT_FOUND("ENV_FILE_NOT_FOUND", HttpStatus.INTERNAL_SERVER_ERROR);
    private final String message;
    private final HttpStatus httpStatus;

    GeoErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
