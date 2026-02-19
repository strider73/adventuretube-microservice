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
 * Stress Test for auth-service → member-service reactive pipeline.
 *
 * Runs a single sequential flow at a time: Login → Refresh → Revoke,
 * one after another (no concurrency).
 *
 * Prerequisites:
 * - Auth service running at AUTH_BASE_URL
 * - Member service running
 * - Test user must already exist (registered in setup)
 * - Valid Google credentials in env.mac
 *
 * Run: mvn verify -Dit.test=StressTestIT -pl auth-service
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StressTestIT {

    // Configuration
    private static final String AUTH_BASE_URL = System.getenv("AUTH_BASE_URL") != null
            ? System.getenv("AUTH_BASE_URL")
            : "http://localhost:8010";

    private static final String MEMBER_BASE_URL = System.getenv("MEMBER_BASE_URL") != null
            ? System.getenv("MEMBER_BASE_URL")
            : "http://localhost:8070";

    private static final int TOTAL_FLOWS = 50;               // Total login→refresh→revoke flows (sequential)
    private static final int WARMUP_FLOWS = 1;             // Warmup flows (not counted)

    // Test data
    private static String googleIdToken;
    private static String testUserEmail;

    // Utilities
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Results storage
    private static final List<FlowResult> flowResults = new ArrayList<>();

    @BeforeAll
    static void setup() {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║     STRESS TEST - SEQUENTIAL (Login→Refresh→Revoke)         ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  Mode:             Sequential (1 flow at a time)            ║");
        log.info("║  Total Flows:      {:>5}                                     ║", TOTAL_FLOWS);
        log.info("║  Warmup Flows:     {:>5}                                     ║", WARMUP_FLOWS);
        log.info("║  Auth URL:  {}                                               ", AUTH_BASE_URL);
        log.info("╚══════════════════════════════════════════════════════════════╝");

        // Load environment
        Map<String, String> env = EnvFileLoader.loadEnvFile("env.mac");
        String clientId = env.get("GOOGLE_CLIENT_ID");
        String clientSecret = env.get("GOOGLE_CLIENT_SECRET");
        String refreshToken = env.get("GOOGLE_REFRESH_TOKEN");

        assertNotNull(clientId, "GOOGLE_CLIENT_ID not found");
        assertNotNull(clientSecret, "GOOGLE_CLIENT_SECRET not found");
        assertNotNull(refreshToken, "GOOGLE_REFRESH_TOKEN not found");

        // Fetch Google ID token
        log.info("Fetching Google ID Token...");
        googleIdToken = GoogleTokenUtil.fetchIdToken(clientId, clientSecret, refreshToken);
        assertNotNull(googleIdToken, "Failed to fetch Google ID Token");
        log.info("Google ID Token fetched successfully");

        // Extract email from token payload and register user
        try {
            String[] parts = googleIdToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode payloadJson = objectMapper.readTree(payload);
            testUserEmail = payloadJson.get("email").asText();
            String name = payloadJson.has("name") ? payloadJson.get("name").asText() : "TestUser";
            log.info("Test user email: {}", testUserEmail);

            // Clean up existing user
            deleteUser(testUserEmail);

            // Register user
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "googleIdToken", googleIdToken,
                    "email", testUserEmail,
                    "password", "password123",
                    "username", name.replaceAll("\\s+", ""),
                    "role", "USER"
            ));
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            restTemplate.exchange(AUTH_BASE_URL + "/auth/users", HttpMethod.POST, request, String.class);
            log.info("Setup complete - test user registered");
        } catch (Exception e) {
            log.error("Setup failed: {}", e.getMessage());
            throw new RuntimeException("Failed to set up test user", e);
        }
    }

    @AfterAll
    static void cleanup() {
        if (testUserEmail != null) {
            log.info("=== StressTestIT Cleanup ===");
            deleteUser(testUserEmail);
        }
    }

    // ============ Tests ============

    @Test
    @Order(1)
    @DisplayName("Warmup - Prime connection pools with full flows")
    void warmup() {
        log.info("=== WARMUP PHASE ({} flows) ===", WARMUP_FLOWS);

        for (int i = 0; i < WARMUP_FLOWS; i++) {
            FlowResult result = executeFullFlow(i + 1);
            log.info("Warmup flow {} - login: {}ms, refresh: {}ms, revoke: {}ms, success: {}",
                    i + 1, result.loginDurationMs, result.refreshDurationMs, result.revokeDurationMs, result.success);
        }

        log.info("Warmup complete. Starting stress test...\n");
    }

    @Test
    @Order(2)
    @DisplayName("Stress Test - Sequential Full Flows (Login → Refresh → Revoke)")
    void stressTestFullFlow() {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║               STARTING STRESS TEST                          ║");
        log.info("║        Single sequential flow: Login → Refresh → Revoke     ║");
        log.info("║        One flow at a time, no concurrency                   ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        int successCount = 0;
        int failureCount = 0;

        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < TOTAL_FLOWS; i++) {
            int flowId = i + 1;
            log.info("Starting flow {}/{}", flowId, TOTAL_FLOWS);

            FlowResult result = executeFullFlow(flowId);
            flowResults.add(result);

            if (result.success) {
                successCount++;
                log.info("Flow {}/{} - SUCCESS - login: {}ms, refresh: {}ms, revoke: {}ms",
                        flowId, TOTAL_FLOWS, result.loginDurationMs, result.refreshDurationMs, result.revokeDurationMs);
            } else {
                failureCount++;
                log.error("Flow {}/{} - FAILED - {}", flowId, TOTAL_FLOWS, result.errorMessage);
            }
        }

        long totalTestDuration = System.currentTimeMillis() - testStartTime;

        generateReport(totalTestDuration, successCount, failureCount);
    }

    // ============ Flow Execution ============

    /**
     * Executes a complete sequential flow: Login → Refresh → Revoke.
     * This mirrors real user behavior — one operation at a time per user.
     */
    private FlowResult executeFullFlow(int flowId) {
        long totalStart = System.currentTimeMillis();
        long loginDuration = 0;
        long refreshDuration = 0;
        long revokeDuration = 0;

        try {
            // STEP 1: Login
            long loginStart = System.currentTimeMillis();
            HttpHeaders loginHeaders = new HttpHeaders();
            loginHeaders.setContentType(MediaType.APPLICATION_JSON);
            String loginBody = "{\"googleIdToken\": \"" + googleIdToken + "\"}";
            HttpEntity<String> loginRequest = new HttpEntity<>(loginBody, loginHeaders);

            ResponseEntity<String> loginResponse = restTemplate.exchange(
                    AUTH_BASE_URL + "/auth/token",
                    HttpMethod.POST,
                    loginRequest,
                    String.class
            );
            loginDuration = System.currentTimeMillis() - loginStart;

            if (!loginResponse.getStatusCode().is2xxSuccessful()) {
                return new FlowResult(flowId, false, loginDuration, 0, 0, 0, "Login failed: " + loginResponse.getStatusCode());
            }

            JsonNode loginJson = objectMapper.readTree(loginResponse.getBody());
            String refreshToken = loginJson.get("refreshToken").asText();

            // STEP 2: Refresh Token
            long refreshStart = System.currentTimeMillis();
            HttpHeaders refreshHeaders = new HttpHeaders();
            refreshHeaders.setContentType(MediaType.APPLICATION_JSON);
            refreshHeaders.set("Authorization", refreshToken);
            HttpEntity<String> refreshRequest = new HttpEntity<>(refreshHeaders);

            ResponseEntity<String> refreshResponse = restTemplate.exchange(
                    AUTH_BASE_URL + "/auth/token/refresh",
                    HttpMethod.POST,
                    refreshRequest,
                    String.class
            );
            refreshDuration = System.currentTimeMillis() - refreshStart;

            if (!refreshResponse.getStatusCode().is2xxSuccessful()) {
                return new FlowResult(flowId, false, loginDuration, refreshDuration, 0, 0, "Refresh failed: " + refreshResponse.getStatusCode());
            }

            // Get new refresh token for revoke
            JsonNode refreshJson = objectMapper.readTree(refreshResponse.getBody());
            String newRefreshToken = refreshJson.get("refreshToken").asText();

            // STEP 3: Revoke Token (Logout)
            long revokeStart = System.currentTimeMillis();
            HttpHeaders revokeHeaders = new HttpHeaders();
            revokeHeaders.set("Authorization", newRefreshToken);
            HttpEntity<String> revokeRequest = new HttpEntity<>(revokeHeaders);

            ResponseEntity<String> revokeResponse = restTemplate.exchange(
                    AUTH_BASE_URL + "/auth/token/revoke",
                    HttpMethod.POST,
                    revokeRequest,
                    String.class
            );
            revokeDuration = System.currentTimeMillis() - revokeStart;

            long totalDuration = System.currentTimeMillis() - totalStart;

            boolean success = revokeResponse.getStatusCode().is2xxSuccessful();
            if (!success) {
                return new FlowResult(flowId, false, loginDuration, refreshDuration, revokeDuration, totalDuration,
                        "Revoke failed: " + revokeResponse.getStatusCode());
            }

            return new FlowResult(flowId, true, loginDuration, refreshDuration, revokeDuration, totalDuration, null);

        } catch (HttpClientErrorException e) {
            long totalDuration = System.currentTimeMillis() - totalStart;
            return new FlowResult(flowId, false, loginDuration, refreshDuration, revokeDuration, totalDuration,
                    "HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - totalStart;
            return new FlowResult(flowId, false, loginDuration, refreshDuration, revokeDuration, totalDuration, e.getMessage());
        }
    }

    // ============ Report Generation ============

    private void generateReport(long totalDuration, int successCount, int failureCount) {
        log.info("\n");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║                    STRESS TEST REPORT                        ║");
        log.info("║         Full Flow: Login → Refresh → Revoke                 ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        List<FlowResult> successfulFlows = flowResults.stream()
                .filter(r -> r.success)
                .collect(Collectors.toList());

        if (successfulFlows.isEmpty()) {
            log.error("No successful flows - cannot generate statistics");
            logFailures();
            return;
        }

        // Per-step statistics
        List<Long> loginTimes = successfulFlows.stream().map(r -> r.loginDurationMs).sorted().collect(Collectors.toList());
        List<Long> refreshTimes = successfulFlows.stream().map(r -> r.refreshDurationMs).sorted().collect(Collectors.toList());
        List<Long> revokeTimes = successfulFlows.stream().map(r -> r.revokeDurationMs).sorted().collect(Collectors.toList());
        List<Long> totalTimes = successfulFlows.stream().map(r -> r.totalDurationMs).sorted().collect(Collectors.toList());

        double throughput = (double) TOTAL_FLOWS / (totalDuration / 1000.0);

        log.info("");
        log.info("┌──────────────────────────────────────────────────────────────┐");
        log.info("│ TEST CONFIGURATION                                           │");
        log.info("├──────────────────────────────────────────────────────────────┤");
        log.info("│ Mode:                 Sequential (1 at a time)              │");
        log.info("│ Total Flows:          {:>6}                                 │", TOTAL_FLOWS);
        log.info("│ Flow Pattern:         Login → Refresh → Revoke              │");
        log.info("└──────────────────────────────────────────────────────────────┘");

        log.info("");
        log.info("┌──────────────────────────────────────────────────────────────┐");
        log.info("│ RESULTS SUMMARY                                              │");
        log.info("├──────────────────────────────────────────────────────────────┤");
        log.info("│ Successful Flows:     {:>6} ({:>5.1f}%)                       │", successCount, (successCount * 100.0 / TOTAL_FLOWS));
        log.info("│ Failed Flows:         {:>6} ({:>5.1f}%)                       │", failureCount, (failureCount * 100.0 / TOTAL_FLOWS));
        log.info("│ Total Test Duration:  {:>6} ms                              │", totalDuration);
        log.info("│ Throughput:           {:>6.1f} flows/sec                     │", throughput);
        log.info("└──────────────────────────────────────────────────────────────┘");

        log.info("");
        log.info("┌──────────────────────────────────────────────────────────────┐");
        log.info("│ RESPONSE TIME BY STEP (ms)                                   │");
        log.info("├──────────────────────────────────────────────────────────────┤");
        log.info("│                   Avg       P50       P90       P99    Max   │");
        log.info("│ Login:       {:>7.1f}   {:>7}   {:>7}   {:>7}  {:>7} │", avg(loginTimes), getPercentile(loginTimes, 50), getPercentile(loginTimes, 90), getPercentile(loginTimes, 99), loginTimes.get(loginTimes.size() - 1));
        log.info("│ Refresh:     {:>7.1f}   {:>7}   {:>7}   {:>7}  {:>7} │", avg(refreshTimes), getPercentile(refreshTimes, 50), getPercentile(refreshTimes, 90), getPercentile(refreshTimes, 99), refreshTimes.get(refreshTimes.size() - 1));
        log.info("│ Revoke:      {:>7.1f}   {:>7}   {:>7}   {:>7}  {:>7} │", avg(revokeTimes), getPercentile(revokeTimes, 50), getPercentile(revokeTimes, 90), getPercentile(revokeTimes, 99), revokeTimes.get(revokeTimes.size() - 1));
        log.info("│ Full Flow:   {:>7.1f}   {:>7}   {:>7}   {:>7}  {:>7} │", avg(totalTimes), getPercentile(totalTimes, 50), getPercentile(totalTimes, 90), getPercentile(totalTimes, 99), totalTimes.get(totalTimes.size() - 1));
        log.info("└──────────────────────────────────────────────────────────────┘");

        logFailures();
        saveReportToFile(totalDuration, successCount, failureCount, loginTimes, refreshTimes, revokeTimes, totalTimes, throughput);
    }

    private void logFailures() {
        List<FlowResult> failures = flowResults.stream()
                .filter(r -> !r.success)
                .collect(Collectors.toList());

        if (!failures.isEmpty()) {
            log.info("");
            log.info("┌──────────────────────────────────────────────────────────────┐");
            log.info("│ FAILURES ({} total)                                          │", failures.size());
            log.info("├──────────────────────────────────────────────────────────────┤");
            failures.stream().limit(10).forEach(f ->
                    log.info("│ Flow #{}: {}",
                            f.flowId, f.errorMessage != null ? f.errorMessage.substring(0, Math.min(f.errorMessage.length(), 55)) : "unknown"));
            if (failures.size() > 10) {
                log.info("│ ... and {} more failures                                    │", failures.size() - 10);
            }
            log.info("└──────────────────────────────────────────────────────────────┘");
        }
    }

    private void saveReportToFile(long totalDuration, int successCount, int failureCount,
                                   List<Long> loginTimes, List<Long> refreshTimes,
                                   List<Long> revokeTimes, List<Long> totalTimes, double throughput) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "stress-test-report-" + timestamp + ".txt";
        String filepath = "target/" + filename;

        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            writer.println("═══════════════════════════════════════════════════════════════");
            writer.println("       STRESS TEST REPORT - Sequential Flow (Reactive)");
            writer.println("       Login → Refresh → Revoke (one at a time)");
            writer.println("═══════════════════════════════════════════════════════════════");
            writer.println();
            writer.println("Timestamp: " + LocalDateTime.now());
            writer.println("Auth URL:  " + AUTH_BASE_URL);
            writer.println();
            writer.println("TEST CONFIGURATION");
            writer.println("──────────────────────────────────────────────────────────────");
            writer.println("Mode:                 Sequential (1 at a time)");
            writer.println("Total Flows:          " + TOTAL_FLOWS);
            writer.println("Flow Pattern:         Login → Refresh → Revoke");
            writer.println();
            writer.println("RESULTS SUMMARY");
            writer.println("──────────────────────────────────────────────────────────────");
            writer.printf("Successful Flows:     %d (%.1f%%)%n", successCount, (successCount * 100.0 / TOTAL_FLOWS));
            writer.printf("Failed Flows:         %d (%.1f%%)%n", failureCount, (failureCount * 100.0 / TOTAL_FLOWS));
            writer.printf("Total Test Duration:  %d ms%n", totalDuration);
            writer.printf("Throughput:           %.1f flows/sec%n", throughput);
            writer.println();
            writer.println("RESPONSE TIME BY STEP (ms)");
            writer.println("──────────────────────────────────────────────────────────────");
            writer.printf("                   Avg      P50      P90      P99      Max%n");
            writer.printf("Login:          %7.1f  %7d  %7d  %7d  %7d%n", avg(loginTimes), getPercentile(loginTimes, 50), getPercentile(loginTimes, 90), getPercentile(loginTimes, 99), loginTimes.get(loginTimes.size() - 1));
            writer.printf("Refresh:        %7.1f  %7d  %7d  %7d  %7d%n", avg(refreshTimes), getPercentile(refreshTimes, 50), getPercentile(refreshTimes, 90), getPercentile(refreshTimes, 99), refreshTimes.get(refreshTimes.size() - 1));
            writer.printf("Revoke:         %7.1f  %7d  %7d  %7d  %7d%n", avg(revokeTimes), getPercentile(revokeTimes, 50), getPercentile(revokeTimes, 90), getPercentile(revokeTimes, 99), revokeTimes.get(revokeTimes.size() - 1));
            writer.printf("Full Flow:      %7.1f  %7d  %7d  %7d  %7d%n", avg(totalTimes), getPercentile(totalTimes, 50), getPercentile(totalTimes, 90), getPercentile(totalTimes, 99), totalTimes.get(totalTimes.size() - 1));
            writer.println();
            writer.println("═══════════════════════════════════════════════════════════════");

            log.info("");
            log.info("Report saved to: {}", filepath);
        } catch (Exception e) {
            log.error("Failed to save report: {}", e.getMessage());
        }
    }

    // ============ Helpers ============

    private static void deleteUser(String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(email, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    MEMBER_BASE_URL + "/member/deleteUser", entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Test user deleted: {}", email);
            }
        } catch (Exception e) {
            log.info("User cleanup skipped (probably not found): {}", e.getMessage());
        }
    }

    private double avg(List<Long> list) {
        return list.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private long getPercentile(List<Long> sortedList, int percentile) {
        if (sortedList.isEmpty()) return 0;
        int index = (int) Math.ceil((percentile / 100.0) * sortedList.size()) - 1;
        index = Math.max(0, Math.min(index, sortedList.size() - 1));
        return sortedList.get(index);
    }

    // ============ Result Holder ============

    private static class FlowResult {
        final int flowId;
        final boolean success;
        final long loginDurationMs;
        final long refreshDurationMs;
        final long revokeDurationMs;
        final long totalDurationMs;
        final String errorMessage;

        FlowResult(int flowId, boolean success, long loginDurationMs, long refreshDurationMs,
                   long revokeDurationMs, long totalDurationMs, String errorMessage) {
            this.flowId = flowId;
            this.success = success;
            this.loginDurationMs = loginDurationMs;
            this.refreshDurationMs = refreshDurationMs;
            this.revokeDurationMs = revokeDurationMs;
            this.totalDurationMs = totalDurationMs;
            this.errorMessage = errorMessage;
        }
    }
}
