package com.adventuretube.geospatial.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final String DOTENV_PROPERTY_SOURCE_NAME = "dotenvProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "default";
        String envFilename = "env." + activeProfile;
        System.out.println("Loading environment variables from: " + envFilename);

        try {
            Dotenv dotenv = Dotenv.configure().filename(envFilename).load();
            Map<String, Object> dotenvProperties = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                dotenvProperties.put(entry.getKey(), entry.getValue());
                System.out.println("Loaded " + entry.getKey() + " = " + entry.getValue());
            });

            MapPropertySource propertySource = new MapPropertySource(DOTENV_PROPERTY_SOURCE_NAME, dotenvProperties);
            environment.getPropertySources().addFirst(propertySource);
            System.out.println("Successfully loaded environment variables from " + envFilename + " into Spring Environment");
        } catch (Exception e) {
            System.out.println("Failed to load .env file from path: " + envFilename + " due to " + e);
        }
    }
}
