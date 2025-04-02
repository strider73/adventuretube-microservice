package com.adventuretube.web;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        servers = {
                @Server(url = "https://api.adventuretube.net", description = "web Service API")
        }
)
public class WebServiceApplication {
    public static void main(String[] args) {
        System.out.println("WebApplication!");
        SpringApplication.run(WebServiceApplication.class);
    }
}