package com.adventuretube.auth;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.adventuretube.auth",
        "com.adventuretube.common"
})
@OpenAPIDefinition(
        servers = {
                @Server(url = "https://api.adventuretube.net", description = "Auth Service API")
        }
)
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class);
    }
}