package com.adventuretube.apigateway.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "springdoc.swagger-ui")
public class SwaggerUrlsProperties {
    private List<SwaggerUrl> urls;

    @Data
    public static class SwaggerUrl {
        private String name;
        private String url;
    }
}