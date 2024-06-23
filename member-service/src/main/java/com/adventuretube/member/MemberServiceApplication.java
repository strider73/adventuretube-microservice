package com.adventuretube.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {"com.adventuretube.common.domain.dto.member", "com.adventuretube.common.domain.dto.token"})
public class MemberServiceApplication {
    public static void main(String[] args) {
        System.out.println("MemberServiceApplication!");
        SpringApplication.run(MemberServiceApplication.class);
    }
}