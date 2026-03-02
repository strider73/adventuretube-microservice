package com.adventuretube.member;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@OpenAPIDefinition(
        servers = {
                @Server(url = "https://api.travel-tube.com", description = "member Service API")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@EntityScan(basePackages = {"com.adventuretube.member.model"})
public class MemberServiceApplication {
    public static void main(String[] args) {
        System.out.println("MemberServiceApplication!");
        SpringApplication.run(MemberServiceApplication.class);
    }
}