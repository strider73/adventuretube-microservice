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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stress Test for member-service via auth-service.
 *
 * This test measures performance BEFORE WebFlux migration.
 * Run again AFTER migration to compare results.
 *
 * Flow tested:
 * 1. Login (POST /auth/token) - calls member-service to validate user
 *
 * Prerequisites:
 * - Auth service running at AUTH_BASE_URL
 * - Member service running
 * - Test user must already exist (register first via LoginFlowModuleApiIT)
 * - Valid Google credentials in env.mac
 *
 * Run: mvn verify -Dit.test=StressTestIT -pl auth-service
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StressTestIT {

    // Configuration - Switch between local and remote
    // private static final String AUTH_BASE_URL = "http://localhost:8010";  // Local
    private static final String AUTH_BASE_URL = "https://api.adventuretube.net";  // Production via Gateway

    private static final int CONCURRENT_USERS = 50;       // Number of concurrent requests
    private static final int TOTAL_REQUESTS = 200;       // Total requests to send
    private static final int WARMUP_REQUESTS = 10;        // Warmup requests (not counted)

    // Test data
    private static String googleIdToken;
    private static String clientId;
    private static String clientSecret;
    private static String refreshToken;

    // Utilities
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Results storage
    private static final List<RequestResult> results = Collections.synchronizedList(new ArrayList<>());

    @BeforeAll
    static void setup() {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           STRESS TEST - BEFORE WEBFLUX MIGRATION             ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║  Concurrent Users: {:>5}                                     ║", CONCURRENT_USERS);
        log.info("║  Total Requests:   {:>5}                                     ║", TOTAL_REQUESTS);
        log.info("║  Warmup Requests:  {:>5}                                     ║", WARMUP_REQUESTS);
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
        log.info("=== WARMUP PHASE ({} requests) ===", WARMUP_REQUESTS);

        for (int i = 0; i < WARMUP_REQUESTS; i++) {
            try {
                executeLoginRequest();
                log.info("Warmup request {} completed", i + 1);
            } catch (Exception e) {
                log.warn("Warmup request {} failed: {}", i + 1, e.getMessage());
            }
        }

        log.info("Warmup complete. Starting stress test...\n");
    }

    @Test
    @Order(2)
    @DisplayName("Stress Test - Concurrent Login Requests")
    void stressTestLogin() throws InterruptedException {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║               STARTING STRESS TEST                           ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long testStartTime = System.currentTimeMillis();

        // Submit all requests
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final int requestId = i + 1;
            executor.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    boolean success = executeLoginRequest();
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    results.add(new RequestResult(requestId, success, duration, null));

                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }

                    int completed = completedCount.incrementAndGet();
                    if (completed % 20 == 0) {
                        log.info("Progress: {}/{} requests completed", completed, TOTAL_REQUESTS);
                    }
                } catch (Exception e) {
                    results.add(new RequestResult(requestId, false, 0, e.getMessage()));
                    failureCount.incrementAndGet();
                    completedCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all requests to complete
        boolean completed = latch.await(5, TimeUnit.MINUTES);
        long testEndTime = System.currentTimeMillis();
        long totalTestDuration = testEndTime - testStartTime;

        executor.shutdown();

        if (!completed) {
            log.error("Test timed out - not all requests completed");
        }

        // Generate report
        generateReport(totalTestDuration, successCount.get(), failureCount.get());
    }

    private boolean executeLoginRequest() {
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

            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            // 4xx errors - request reached server but was rejected
            return false;
        } catch (Exception e) {
            // Connection errors, timeouts, etc.
            return false;
        }
    }

    private void generateReport(long totalDuration, int successCount, int failureCount) {
        log.info("\n");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║                    STRESS TEST REPORT                        ║");
        log.info("║            BEFORE WEBFLUX MIGRATION (Servlet + JPA)          ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        // Calculate statistics
        List<Long> successfulDurations = results.stream()
                .filter(r -> r.success)
                .map(r -> r.durationMs)
                .sorted()
                .collect(Collectors.toList());

        if (successfulDurations.isEmpty()) {
            log.error("No successful requests - cannot generate statistics");
            return;
        }

        long minTime = successfulDurations.get(0);
        long maxTime = successfulDurations.get(successfulDurations.size() - 1);
        double avgTime = successfulDurations.stream().mapToLong(Long::longValue).average().orElse(0);
        long p50 = getPercentile(successfulDurations, 50);
        long p90 = getPercentile(successfulDurations, 90);
        long p95 = getPercentile(successfulDurations, 95);
        long p99 = getPercentile(successfulDurations, 99);
        double throughput = (double) TOTAL_REQUESTS / (totalDuration / 1000.0);

        log.info("");
        log.info("┌──────────────────────────────────────────────────────────────┐");
        log.info("│ TEST CONFIGURATION                                           │");
        log.info("├──────────────────────────────────────────────────────────────┤");
        log.info("│ Concurrent Users:     {:>6}                                 │", CONCURRENT_USERS);
        log.info("│ Total Requests:       {:>6}                                 │", TOTAL_REQUESTS);
        log.info("│ Target Endpoint:      POST /auth/token (login)              │");
        log.info("└──────────────────────────────────────────────────────────────┘");

        log.info("");
        log.info("┌──────────────────────────────────────────────────────────────┐");
        log.info("│ RESULTS SUMMARY                                              │");
        log.info("├──────────────────────────────────────────────────────────────┤");
        log.info("│ Successful Requests:  {:>6} ({:>5.1f}%)                       │", successCount, (successCount * 100.0 / TOTAL_REQUESTS));
        log.info("│ Failed Requests:      {:>6} ({:>5.1f}%)                       │", failureCount, (failureCount * 100.0 / TOTAL_REQUESTS));
        log.info("│ Total Test Duration:  {:>6} ms                              │", totalDuration);
        log.info("│ Throughput:           {:>6.1f} req/sec                       │", throughput);
        log.info("└──────────────────────────────────────────────────────────────┘");

        log.info("");
        log.info("┌──────────────────────────────────────────────────────────────┐");
        log.info("│ RESPONSE TIME (ms)                                           │");
        log.info("├──────────────────────────────────────────────────────────────┤");
        log.info("│ Min:                  {:>6} ms                              │", minTime);
        log.info("│ Max:                  {:>6} ms                              │", maxTime);
        log.info("│ Average:              {:>6.1f} ms                              │", avgTime);
        log.info("│ Median (P50):         {:>6} ms                              │", p50);
        log.info("│ P90:                  {:>6} ms                              │", p90);
        log.info("│ P95:                  {:>6} ms                              │", p95);
        log.info("│ P99:                  {:>6} ms                              │", p99);
        log.info("└──────────────────────────────────────────────────────────────┘");

        // Save to file for comparison
        saveReportToFile(totalDuration, successCount, failureCount, minTime, maxTime, avgTime, p50, p90, p95, p99, throughput);
    }

    private void saveReportToFile(long totalDuration, int successCount, int failureCount,
                                   long minTime, long maxTime, double avgTime,
                                   long p50, long p90, long p95, long p99, double throughput) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String filename = "stress-test-report-BEFORE-" + timestamp + ".txt";
        String filepath = "target/" + filename;

        try (PrintWriter writer = new PrintWriter(new FileWriter(filepath))) {
            writer.println("═══════════════════════════════════════════════════════════════");
            writer.println("       STRESS TEST REPORT - BEFORE WEBFLUX MIGRATION");
            writer.println("              (Servlet + JPA / Tomcat + JDBC)");
            writer.println("═══════════════════════════════════════════════════════════════");
            writer.println();
            writer.println("Timestamp: " + LocalDateTime.now());
            writer.println();
            writer.println("TEST CONFIGURATION");
            writer.println("──────────────────────────────────────────────────────────────");
            writer.println("Concurrent Users:     " + CONCURRENT_USERS);
            writer.println("Total Requests:       " + TOTAL_REQUESTS);
            writer.println("Target Endpoint:      POST /auth/token (login)");
            writer.println("Flow:                 auth-service → member-service → PostgreSQL");
            writer.println();
            writer.println("RESULTS SUMMARY");
            writer.println("──────────────────────────────────────────────────────────────");
            writer.printf("Successful Requests:  %d (%.1f%%)%n", successCount, (successCount * 100.0 / TOTAL_REQUESTS));
            writer.printf("Failed Requests:      %d (%.1f%%)%n", failureCount, (failureCount * 100.0 / TOTAL_REQUESTS));
            writer.printf("Total Test Duration:  %d ms%n", totalDuration);
            writer.printf("Throughput:           %.1f req/sec%n", throughput);
            writer.println();
            writer.println("RESPONSE TIME (ms)");
            writer.println("──────────────────────────────────────────────────────────────");
            writer.printf("Min:                  %d ms%n", minTime);
            writer.printf("Max:                  %d ms%n", maxTime);
            writer.printf("Average:              %.1f ms%n", avgTime);
            writer.printf("Median (P50):         %d ms%n", p50);
            writer.printf("P90:                  %d ms%n", p90);
            writer.printf("P95:                  %d ms%n", p95);
            writer.printf("P99:                  %d ms%n", p99);
            writer.println();
            writer.println("═══════════════════════════════════════════════════════════════");
            writer.println("Save this report to compare with AFTER migration results.");
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

    // Result holder
    private static class RequestResult {
        final int requestId;
        final boolean success;
        final long durationMs;
        final String errorMessage;

        RequestResult(int requestId, boolean success, long durationMs, String errorMessage) {
            this.requestId = requestId;
            this.success = success;
            this.durationMs = durationMs;
            this.errorMessage = errorMessage;
        }
    }
}
