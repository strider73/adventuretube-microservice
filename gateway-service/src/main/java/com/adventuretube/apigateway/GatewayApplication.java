package com.adventuretube.apigateway;

import com.adventuretube.apigateway.config.SwaggerUrlsProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class GatewayApplication {

    private final SwaggerUrlsProperties swaggerUrlsProperties;

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @PostConstruct
    public void logSwaggerUrls() {
        System.out.println("✅ Loaded Swagger UI URLs:");
        swaggerUrlsProperties.getUrls().forEach(url ->
                System.out.println(" - " + url.getName() + ": " + url.getUrl()));
    }
}
