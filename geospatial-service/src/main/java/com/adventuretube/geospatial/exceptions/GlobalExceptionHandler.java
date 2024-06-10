package com.adventuretube.geospatial.exceptions;


import com.adventuretube.common.error.RestAPIErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestAPIErrorResponse> handleUnknownException(Exception ex) {
        RestAPIErrorResponse restAPIErrorResponse = new RestAPIErrorResponse(
                ex.getMessage(),
                "Internal Server Error",
                INTERNAL_SERVER_ERROR.value(),
                System.currentTimeMillis()
           );

            return new ResponseEntity<>(restAPIErrorResponse, INTERNAL_SERVER_ERROR);
    }
}
