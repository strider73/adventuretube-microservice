package com.adventuretube.common.error;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private String details;
    private int statusCode;
    private long timestamp;
}
