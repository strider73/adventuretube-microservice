package com.adventuretube.auth.stress;

import com.adventuretube.auth.support.EnvFileLoader;
import com.adventuretube.auth.support.GoogleTokenUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sequential Flow Test - Single user, repeated cycles.
 *
 * Each cycle: Login → Refresh Token → Logout (Revoke)
 *
 * This avoids duplicate token errors by ensuring each cycle completes
 * before the next one starts.
 *
 * Run: mvn verify -Dit.test=SequentialFlowTestIT -pl auth-service
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SequentialFlowTestIT {

    // Configuration
    // private static final String AUTH_BASE_URL = "http://localhost:8010";  // Local
    private static final String AUTH_BASE_URL = "https://api.adventuretube.net";  // Production via Gateway

    private static final int TOTAL_CYCLES = 50;      // Number of full cycles to run
    private static final int WARMUP_CYCLES = 3;      // Warmup cycles (not counted in stats)

    // Test data
    private static String googleIdToken;
    private static String clientId;
    private static String clientSecret;
    private static String refreshToken;

    // Utilities
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Results storage
    private static final List<CycleResult> results = new ArrayList<>();

    @BeforeAll
    static void setup() {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║         SEQUENTIAL FLOW TEST - AFTER WEBFLUX MIGRATION       ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  Total Cycles:       {:>5}                                   ║", TOTAL_CYCLES);
        log.info("║  Warmup Cycles:      {:>5}                                   ║", WARMUP_CYCLES);
        log.info("║  Flow: Login → Refresh → Logout                              ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        // Load environment
        Map<String, String> env = EnvFileLoader.loadEnvFile("env.mac");
        clientId = env.get("GOOGLE_CLIENT_ID");
        clientSecret = env.get("GOOGLE_CLIENT_SECRET");
        refreshToken = env.get("GOOGLE_REFRESH_TOKEN");

        assertNotNull(clientId, "GOOGLE_CLIENT_ID not found");
        assertNotNull(clientSecret, "GOOGLE_CLIENT_SECRET not found");
        assertNotNull(refreshToken, "GOOGLE_REFRESH_TOKEN not found");

        // Fetch Google ID token
        log.info("Fetching Google ID Token...");
        googleIdToken = GoogleTokenUtil.fetchIdToken(clientId, clientSecret, refreshToken);
        assertNotNull(googleIdToken, "Failed to fetch Google ID Token");
        log.info("Google ID Token fetched successfully");
    }

    @Test
    @Order(1)
    @DisplayName("Warmup - Prime the connection pools")
    void warmup() {
        log.info("=== WARMUP PHASE ({} cycles) ===", WARMUP_CYCLES);

        for (int i = 0; i < WARMUP_CYCLES; i++) {
            try {
                CycleResult result = executeFullCycle(i + 1);
                log.info("Warmup cycle {} completed - Login: {}ms, Refresh: {}ms, Logout: {}ms",
                        i + 1, result.loginDurationMs, result.refreshDurationMs, result.logoutDurationMs);
            } catch (Exception e) {
                log.warn("Warmup cycle {} failed: {}", i + 1, e.getMessage());
            }
        }

        log.info("Warmup complete. Starting test cycles...\n");
    }

    @Test
    @Order(2)
    @DisplayName("Sequential Flow Test - Login → Refresh → Logout cycles")
    void sequentialFlowTest() {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║             STARTING SEQUENTIAL FLOW TEST                    ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        long testStartTime = System.currentTimeMillis();
        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < TOTAL_CYCLES; i++) {
            int cycleNum = i + 1;
            try {
                CycleResult result = executeFullCycle(cycleNum);
                results.add(result);

                if (result.success) {
                    successCount++;
                } else {
                    failureCount++;
                }

                if (cycleNum % 10 == 0) {
                    log.info("Progress: {}/{} cycles completed", cycleNum, TOTAL_CYCLES);
                }
            } catch (Exception e) {
                log.error("Cycle {} failed with exception: {}", cycleNum, e.getMessage());
                results.add(new CycleResult(cycleNum, false, 0, 0, 0, e.getMessage()));
                failureCount++;
            }
        }

        long testEndTime = System.currentTimeMillis();
        long totalTestDuration = testEndTime - testStartTime;

        // Generate report
        generateReport(totalTestDuration, successCount, failureCount);
    }

    private CycleResult executeFullCycle(int cycleNum) {
        long loginStart, loginEnd, refreshStart, refreshEnd, logoutStart, logoutEnd;
        String accessToken = null;
        String appRefreshToken = null;
        String errorMessage = null;
        boolean success = true;

        // Step 1: Login
        loginStart = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{\"googleIdToken\": \"" + googleIdToken + "\"}";
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    AUTH_BASE_URL + "/auth/token",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode json = objectMapper.readTree(response.getBody());
                accessToken = json.path("accessToken").asText();
                appRefreshToken = json.path("refreshToken").asText();
            } else {
                success = false;
                errorMessage = "Login failed: " + response.getStatusCode();
            }
        } catch (Exception e) {
            success = false;
            errorMessage = "Login exception: " + e.getMessage();
        }
        loginEnd = System.currentTimeMillis();

        if (!success || accessToken == null) {
            return new CycleResult(cycleNum, false, loginEnd - loginStart, 0, 0, errorMessage);
        }

        // Step 2: Refresh Token
        refreshStart = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Send raw token without "Bearer " prefix - filter expects raw JWT
            headers.set("Authorization", appRefreshToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    AUTH_BASE_URL + "/auth/token/refresh",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode json = objectMapper.readTree(response.getBody());
                accessToken = json.path("accessToken").asText();
                appRefreshToken = json.path("refreshToken").asText();
            } else {
                success = false;
                errorMessage = "Refresh failed: " + response.getStatusCode();
            }
        } catch (Exception e) {
            success = false;
            errorMessage = "Refresh exception: " + e.getMessage();
        }
        refreshEnd = System.currentTimeMillis();

        if (!success) {
            return new CycleResult(cycleNum, false, loginEnd - loginStart,
                    refreshEnd - refreshStart, 0, errorMessage);
        }

        // Step 3: Logout (Revoke Token)
        logoutStart = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Send raw token without "Bearer " prefix - filter expects raw JWT
            headers.set("Authorization", accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    AUTH_BASE_URL + "/auth/token/revoke",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                success = false;
                errorMessage = "Logout failed: " + response.getStatusCode();
            }
        } catch (Exception e) {
            success = false;
            errorMessage = "Logout exception: " + e.getMessage();
        }
        logoutEnd = System.currentTimeMillis();

        return new CycleResult(cycleNum, success,
                loginEnd - loginStart,
                refreshEnd - refreshStart,
                logoutEnd - logoutStart,
                errorMessage);
    }

    private void generateReport(long totalDuration, int successCount, int failureCount) {
        log.info("\n");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║                 SEQUENTIAL FLOW TEST REPORT                  ║");
        log.info("║            AFTER WEBFLUX MIGRATION (WebFlux + R2DBC)         ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        List<CycleResult> successfulResults = results.stream()
                .filter(r -> r.success)
                .collect(Collectors.toList());

        if (successfulResults.isEmpty()) {
            log.error("No successful cycles - cannot generate statistics");
            return;
        }

        // Calculate statistics for each phase
        List<Long> loginTimes = successfulResults.stream().map(r -> r.loginDurationMs).sorted().collect(Collectors.toList());
        List<Long> refreshTimes = successfulResults.stream().map(r -> r.refreshDurationMs).sorted().collect(Collectors.toList());
        List<Long> logoutTimes = successfulResults.stream().map(r -> r.logoutDurationMs).sorted().collect(Collectors.toList());
        List<Long> totalTimes = successfulResults.stream().map(r -> r.loginDurationMs + r.refreshDurationMs + r.logoutDurationMs).sorted().collect(Collectors.toList());

        double throughput = (double) TOTAL_CYCLES / (totalDuration / 1000.0);
        double cyclesPerSecond = (double) successCount / (totalDuration / 1000.0);

        log.info("");
        log.info("┌──────────────────────────────────────────────────────────────┐");
        log.info("│ TEST CONFIGURATION                                           │");
        log.info("├──────────────────────────────────────────────────────────────┤");
        log.info("│ Total Cycles:         {:>6}                                 │", TOTAL_CYCLES);
        log.info("│ Flow:                 Login → Refresh → Logout              │");
        log.info("│ Mode:                 Sequential (single-threaded)          │");
        log.info("└──────────────────────────────────────────────────────────────┘");

        log.info("");
        log.info("┌──────────────────────────────────────────────────────────────┐");
        log.info("│ RESULTS SUMMARY                                              │");
        log.info("├──────────────────────────────────────────────────────────────┤");
        log.info("│ Successful Cycles:    {:>6} ({:>5.1f}%)                       │", successCount, (successCount * 100.0 / TOTAL_CYCLES));
        log.info("│ Failed Cycles:        {:>6} ({:>5.1f}%)                       │", failureCount, (failureCount * 100.0 / TOTAL_CYCLES));
        log.info("│ Total Test Duration:  {:>6} ms                              │", totalDuration);
        log.info("│ Cycles/sec:           {:>6.2f}                                │", cyclesPerSecond);
        log.info("└──────────────────────────────────────────────────────────────┘");

        log.info("");
        log.info("┌──────────────────────────────────────────────────────────────┐");
        log.info("│ LOGIN RESPONSE TIME (ms)                                     │");
        log.info("├──────────────────────────────────────────────────────────────┤");
        log.info("│ Min:    {:>6} │ Max:    {:>6} │ Avg:    {:>6.1f}            │",
                loginTimes.get(0), loginTimes.get(loginTimes.size()-1), loginTimes.stream().mapToLong(Long::longValue).average().orElse(0));
        log.info("│ P50:    {:>6} │ P90:    {:>6} │ P99:    {:>6}            │",
                getPercentile(loginTimes, 50), getPercentile(loginTimes, 90), getPercentile(loginTimes, 99));
        log.info("└──────────────────────────────────────────────────────────────┘");

        log.info("");
        log.info("┌──────────────────────────────────────────────────────────────┐");
        log.info("│ REFRESH RESPONSE TIME (ms)                                   │");
        log.info("├──────────────────────────────────────────────────────────────┤");
        log.info("│ Min:    {:>6} │ Max:    {:>6} │ Avg:    {:>6.1f}            │",
                refreshTimes.get(0), refreshTimes.get(refreshTimes.size()-1), refreshTimes.stream().mapToLong(Long::longValue).average().orElse(0));
        log.info("│ P50:    {:>6} │ P90:    {:>6} │ P99:    {:>6}            │",
                getPercentile(refreshTimes, 50), getPercentile(refreshTimes, 90), getPercentile(refreshTimes, 99));
        log.info("└──────────────────────────────────────────────────────────────┘");

        log.info("");
        log.info("┌──────────────────────────────────────────────────────────────┐");
        log.info("│ LOGOUT RESPONSE TIME (ms)                                    │");
        log.info("├──────────────────────────────────────────────────────────────┤");
        log.info("│ Min:    {:>6} │ Max:    {:>6} │ Avg:    {:>6.1f}            │",
                logoutTimes.get(0), logoutTimes.get(logoutTimes.size()-1), logoutTimes.stream().mapToLong(Long::longValue).average().orElse(0));
        log.info("│ P50:    {:>6} │ P90:    {:>6} │ P99:    {:>6}            │",
                getPercentile(logoutTimes, 50), getPercentile(logoutTimes, 90), getPercentile(logoutTimes, 99));
        log.info("└──────────────────────────────────────────────────────────────┘");

        log.info("");
        log.info("┌──────────────────────────────────────────────────────────────┐");
        log.info("│ FULL CYCLE TIME (ms)                                         │");
        log.info("├──────────────────────────────────────────────────────────────┤");
        log.info("│ Min:    {:>6} │ Max:    {:>6} │ Avg:    {:>6.1f}            │",
                totalTimes.get(0), totalTimes.get(totalTimes.size()-1), totalTimes.stream().mapToLong(Long::longValue).average().orElse(0));
        log.info("│ P50:    {:>6} │ P90:    {:>6} │ P99:    {:>6}            │",
                getPercentile(totalTimes, 50), getPercentile(totalTimes, 90), getPercentile(totalTimes, 99));
        log.info("└──────────────────────────────────────────────────────────────┘");

        // Save to file
        saveReportToFile(totalDuration, successCount, failureCount, cyclesPerSecond,
                loginTimes, refreshTimes, logoutTimes, totalTimes);
    }

    private void saveReportToFile(long totalDuration, int successCount, int failureCount,
                                   double cyclesPerSecond, List<Long> loginTimes,
                                   List<Long> refreshTimes, List<Long> logoutTimes, List<Long> totalTimes) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "sequential-flow-test-AFTER-" + timestamp + ".txt";
        String filepath = "target/" + filename;

        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            writer.println("═══════════════════════════════════════════════════════════════");
            writer.println("       SEQUENTIAL FLOW TEST REPORT - AFTER WEBFLUX MIGRATION");
            writer.println("                    (WebFlux + R2DBC / Netty)");
            writer.println("═══════════════════════════════════════════════════════════════");
            writer.println();
            writer.println("Timestamp: " + LocalDateTime.now());
            writer.println();
            writer.println("TEST CONFIGURATION");
            writer.println("──────────────────────────────────────────────────────────────");
            writer.println("Total Cycles:         " + TOTAL_CYCLES);
            writer.println("Flow:                 Login → Refresh → Logout");
            writer.println("Mode:                 Sequential (single-threaded)");
            writer.println();
            writer.println("RESULTS SUMMARY");
            writer.println("──────────────────────────────────────────────────────────────");
            writer.printf("Successful Cycles:    %d (%.1f%%)%n", successCount, (successCount * 100.0 / TOTAL_CYCLES));
            writer.printf("Failed Cycles:        %d (%.1f%%)%n", failureCount, (failureCount * 100.0 / TOTAL_CYCLES));
            writer.printf("Total Test Duration:  %d ms%n", totalDuration);
            writer.printf("Cycles/sec:           %.2f%n", cyclesPerSecond);
            writer.println();

            writer.println("LOGIN RESPONSE TIME (ms)");
            writer.println("──────────────────────────────────────────────────────────────");
            writer.printf("Min: %d | Max: %d | Avg: %.1f%n",
                    loginTimes.get(0), loginTimes.get(loginTimes.size()-1),
                    loginTimes.stream().mapToLong(Long::longValue).average().orElse(0));
            writer.printf("P50: %d | P90: %d | P99: %d%n",
                    getPercentile(loginTimes, 50), getPercentile(loginTimes, 90), getPercentile(loginTimes, 99));
            writer.println();

            writer.println("REFRESH RESPONSE TIME (ms)");
            writer.println("──────────────────────────────────────────────────────────────");
            writer.printf("Min: %d | Max: %d | Avg: %.1f%n",
                    refreshTimes.get(0), refreshTimes.get(refreshTimes.size()-1),
                    refreshTimes.stream().mapToLong(Long::longValue).average().orElse(0));
            writer.printf("P50: %d | P90: %d | P99: %d%n",
                    getPercentile(refreshTimes, 50), getPercentile(refreshTimes, 90), getPercentile(refreshTimes, 99));
            writer.println();

            writer.println("LOGOUT RESPONSE TIME (ms)");
            writer.println("──────────────────────────────────────────────────────────────");
            writer.printf("Min: %d | Max: %d | Avg: %.1f%n",
                    logoutTimes.get(0), logoutTimes.get(logoutTimes.size()-1),
                    logoutTimes.stream().mapToLong(Long::longValue).average().orElse(0));
            writer.printf("P50: %d | P90: %d | P99: %d%n",
                    getPercentile(logoutTimes, 50), getPercentile(logoutTimes, 90), getPercentile(logoutTimes, 99));
            writer.println();

            writer.println("FULL CYCLE TIME (ms)");
            writer.println("──────────────────────────────────────────────────────────────");
            writer.printf("Min: %d | Max: %d | Avg: %.1f%n",
                    totalTimes.get(0), totalTimes.get(totalTimes.size()-1),
                    totalTimes.stream().mapToLong(Long::longValue).average().orElse(0));
            writer.printf("P50: %d | P90: %d | P99: %d%n",
                    getPercentile(totalTimes, 50), getPercentile(totalTimes, 90), getPercentile(totalTimes, 99));
            writer.println();

            writer.println("═══════════════════════════════════════════════════════════════");
            writer.println("Compare with BEFORE migration results to measure improvement.");
            writer.println("═══════════════════════════════════════════════════════════════");

            log.info("");
            log.info("Report saved to: {}", filepath);
        } catch (Exception e) {
            log.error("Failed to save report: {}", e.getMessage());
        }
    }

    private long getPercentile(List<Long> sortedList, int percentile) {
        if (sortedList.isEmpty()) return 0;
        int index = (int) Math.ceil((percentile / 100.0) * sortedList.size()) - 1;
        index = Math.max(0, Math.min(index, sortedList.size() - 1));
        return sortedList.get(index);
    }

    // Result holder for a full cycle
    private static class CycleResult {
        final int cycleNum;
        final boolean success;
        final long loginDurationMs;
        final long refreshDurationMs;
        final long logoutDurationMs;
        final String errorMessage;

        CycleResult(int cycleNum, boolean success, long loginDurationMs,
                   long refreshDurationMs, long logoutDurationMs, String errorMessage) {
            this.cycleNum = cycleNum;
            this.success = success;
            this.loginDurationMs = loginDurationMs;
            this.refreshDurationMs = refreshDurationMs;
            this.logoutDurationMs = logoutDurationMs;
            this.errorMessage = errorMessage;
        }
    }
}
