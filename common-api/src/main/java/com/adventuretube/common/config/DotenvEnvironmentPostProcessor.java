package com.adventuretube.common.config;


import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads environment variables from a .env file based on the active Spring profile.
 *
 * Priority:
 * 1. Parent directory of the current working directory (e.g., project root in multi-module setups)
 * 2. Current working directory (fallback if not found in parent)
 *
 * Docker-specific:
 * When running in Docker, parentDir may be null (depending on container mount),
 * so the working directory is treated as the root source for .env.
 *
 * Example: if the active profile is 'mac', this looks for 'env.mac' in that order.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DOTENV_PROPERTY_SOURCE_NAME = "dotenvProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        //mot using active profile to determine which env file to load anymre but using a  addition environment variable
        //envTarget to determine which env file to load instead !!!

//        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "default";
//
//        //When test case for unit,mock, integration profile name will be set from test case annotation
//        //which those 3 value has no  env file, and need to fall back to default which has env.mac file
//        if (activeProfile.equals("unit") || activeProfile.equals("mock") || activeProfile.equals("integration")) {
//            activeProfile = "mac";
//        }
//
//        String envFilename = "env." + activeProfile;

        String envFilename = "env."+ System.getenv("envTarget");
        Dotenv dotenv = null;

        System.out.println("Loading environment variables from: " + envFilename);

        try {
            Path workingDir = Paths.get(System.getProperty("user.dir"));
            Path parentDir = workingDir.getParent();

            // Case 1: Parent exists (normal run)
            if (parentDir != null) {
                Path parentEnvPath = parentDir.resolve(envFilename);
                if (Files.exists(parentEnvPath)) {
                    System.out.println("Loading env from parent dir: " + parentEnvPath);
                    dotenv = Dotenv.configure()
                            .directory(parentDir.toString())
                            .filename(envFilename)
                            .load();
                } else {
                    // Case 2: fallback to working dir
                    Path workingEnvPath = workingDir.resolve(envFilename);
                    if (Files.exists(workingEnvPath)) {
                        System.out.println("Loading env from working dir: " + workingEnvPath);
                        dotenv = Dotenv.configure()
                                .directory(workingDir.toString())
                                .filename(envFilename)
                                .load();
                    }
                }
            }
            // Case 3: No parent — running inside Docker or packaged JAR
            else {
                Path workingEnvPath = workingDir.resolve(envFilename);
                if (Files.exists(workingEnvPath)) {
                    System.out.println("Loading env from Docker working dir: " + workingEnvPath);
                    dotenv = Dotenv.configure()
                            .directory(workingDir.toString())
                            .filename(envFilename)
                            .load();
                }
            }

            // Apply loaded dotenv to Spring Environment
            if (dotenv != null) {
                Map<String, Object> dotenvProperties = new HashMap<>();
                dotenv.entries().forEach(entry -> {
                    dotenvProperties.put(entry.getKey(), entry.getValue());
                    System.out.println("Loaded " + entry.getKey() + "=" + entry.getValue());
                });

                environment.getPropertySources().addFirst(
                        new MapPropertySource(DOTENV_PROPERTY_SOURCE_NAME, dotenvProperties)
                );
            } else {
                System.out.println("No .env file found in parent or working directory.");
            }

        } catch (Exception e) {
            System.out.println("Failed to load .env file from path: " + envFilename + " due to " + e);
        }
    }
}
