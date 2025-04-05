package com.adventuretube.auth.config.google;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "google")
public class GoogleTokenCredentialProperties {

    @NotBlank
    private String clientId;
    @NotBlank
    private String clientSecret;
    private String redirectUri;
}