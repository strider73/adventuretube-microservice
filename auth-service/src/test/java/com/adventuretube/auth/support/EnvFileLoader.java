package com.adventuretube.auth.support;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to load environment variables from env.* files.
 * Searches in current directory and parent directories up to project root.
 */
@Slf4j
public class EnvFileLoader {

    public static Map<String, String> loadEnvFile(String filename) {
        Map<String, String> envVars = new HashMap<>();

        Path envFile = findEnvFile(filename);
        if (envFile == null) {
            throw new RuntimeException("Could not find " + filename + " in current or parent directories");
        }

        log.info("Loading environment from: {}", envFile.toAbsolutePath());

        try {
            Files.readAllLines(envFile).forEach(line -> {
                // Skip comments and empty lines
                if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                    return;
                }

                // Parse KEY=VALUE
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    // Remove quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    envVars.put(key, value);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + filename + ": " + e.getMessage(), e);
        }

        log.info("Loaded {} environment variables", envVars.size());
        return envVars;
    }

    private static Path findEnvFile(String filename) {
        // Start from current working directory
        Path current = Paths.get("").toAbsolutePath();

        // Search up to 5 parent directories
        for (int i = 0; i < 5; i++) {
            Path envPath = current.resolve(filename);
            if (Files.exists(envPath)) {
                return envPath;
            }
            current = current.getParent();
            if (current == null) {
                break;
            }
        }

        return null;
    }
}