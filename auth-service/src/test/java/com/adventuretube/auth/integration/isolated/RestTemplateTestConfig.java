package com.adventuretube.auth.mockhttp.service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
public class RestTemplateTestConfig {
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
