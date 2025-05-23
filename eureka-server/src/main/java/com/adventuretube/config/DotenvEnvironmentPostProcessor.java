package com.adventuretube.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;
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
 * <p>
 * Priority:
 * 1. Parent directory of the current working directory (e.g., project root in multi-module setups)
 * 2. Current working directory (fallback if parent not found)
 * <p>
 * This ensures submodules can share a central .env configuration file located
 * in the root project directory, avoiding duplication across modules.
 * <p>
 * Example: if the active profile is 'mac', this looks for 'env.mac' in the parent first,
 * then in the current directory if not found.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String activeProfile = environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "default";
        String envFilename = "env." + activeProfile;
        Dotenv dotenv = null;
        System.out.println("Loading environment variables from: " + envFilename);


        try {
            Path workingDir = Paths.get(System.getProperty("user.dir"));
            Path parentDir = workingDir.getParent();

            // Try parent dir first (if available)
            if (parentDir != null) {
                Path parentEnvPath = parentDir.resolve(envFilename);
                if (Files.exists(parentEnvPath)) {
                    System.out.println("Loading env from parent dir: " + parentEnvPath);
                    dotenv = Dotenv.configure()
                            .directory(parentDir.toString())
                            .filename(envFilename)
                            .load();
                }
            }

            // Fallback to working dir
            if (dotenv == null) {
                Path workingEnvPath = workingDir.resolve(envFilename);
                if (Files.exists(workingEnvPath)) {
                    System.out.println("Loading env from working dir: " + workingEnvPath);
                    dotenv = Dotenv.configure()
                            .directory(workingDir.toString())
                            .filename(envFilename)
                            .load();
                }
            }

        } catch (Exception e) {
            System.out.println("Failed to load .env file from path: " + envFilename + " due to " + e);
        }
    }
}

