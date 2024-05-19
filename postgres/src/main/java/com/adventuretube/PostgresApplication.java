package com.adventuretube;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PostgresApplication {
    public static void main(String[] args) {
        System.out.println("PostgresApplication!");
        SpringApplication.run(PostgresApplication.class);
    }
}