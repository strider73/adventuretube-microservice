package com.adventuretube.geospatial.exceptions.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RestAPIResponse {
    private String message;
    private String details;
    private int statusCode;
    private long timestamp;
}
