package com.adventuretube.geospatial.exceptions;


import com.adventuretube.common.error.CommonErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonErrorResponse> handleUnknownException(Exception ex) {
        CommonErrorResponse commonErrorResponse = new CommonErrorResponse(
                ex.getMessage(),
                "Internal Server Error",
                INTERNAL_SERVER_ERROR.value(),
                System.currentTimeMillis()
           );

            return new ResponseEntity<>(commonErrorResponse, INTERNAL_SERVER_ERROR);
    }
}
