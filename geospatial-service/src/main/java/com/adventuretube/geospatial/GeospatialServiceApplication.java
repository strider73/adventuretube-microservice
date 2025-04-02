package com.adventuretube.geospatial;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@OpenAPIDefinition(
        servers = {
                @Server(url = "https://api.adventuretube.net", description = "geospatial Service API")
        }
)
@SpringBootApplication
public class GeospatialServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GeospatialServiceApplication.class);
    }
}