package com.adventuretube.auth.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);
    private static final String DOTENV_PROPERTY_SOURCE_NAME = "dotenvProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "default";
        String envFilename = "env." + activeProfile;

        try {
            Dotenv dotenv = Dotenv.configure().filename(envFilename).load();
            Map<String, Object> dotenvProperties = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                dotenvProperties.put(entry.getKey(), entry.getValue());
                logger.debug("Loaded {} = {}", entry.getKey(), entry.getValue());
            });

            MapPropertySource propertySource = new MapPropertySource(DOTENV_PROPERTY_SOURCE_NAME, dotenvProperties);
            environment.getPropertySources().addFirst(propertySource);
            logger.info("Loaded environment variables from {} into Spring Environment", envFilename);
        } catch (Exception e) {
            logger.error("Failed to load .env file from path: {}", envFilename, e);
            // Consider whether to fail or continue without these properties
        }
    }
}
