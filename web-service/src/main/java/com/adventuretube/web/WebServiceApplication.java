package com.adventuretube.web;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.adventuretube.web",
        "com.adventuretube.common"
})
@OpenAPIDefinition(
        servers = {
                @Server(url = "https://api.travel-tube.com", description = "web Service API")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class WebServiceApplication {
    public static void main(String[] args) {
        System.out.println("WebApplication!");
        SpringApplication.run(WebServiceApplication.class);
    }
}