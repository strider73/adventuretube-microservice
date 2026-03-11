package com.adventuretube.auth.component.service;

import com.adventuretube.auth.service.GeoDataService;
import com.adventuretube.auth.service.JwtUtil;
import com.adventuretube.common.client.ServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class GeoDataServiceComponentTest {

    private MockWebServer mockWebServer;
    private GeoDataService geoDataService;
    private JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String TEST_EMAIL = "strider.lee@gmail.com";
    private static final String TEST_JWT_SECRET = "dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciB0ZXN0aW5nIHB1cnBvc2VzIG9ubHk=";
    private static final String TEST_ACCESS_EXPIRATION = "3600";
    private static final String TEST_REFRESH_EXPIRATION = "86400";

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();

        WebClient.Builder webClientBuilder = WebClient.builder();

        ReactiveCircuitBreakerFactory<Object, ConfigBuilder<Object>> circuitBreakerFactory =
                new ReactiveCircuitBreakerFactory<>() {
                    @Override
                    public ReactiveCircuitBreaker create(String id) {
                        return new ReactiveCircuitBreaker() {
                            @Override
                            public <T> Mono<T> run(Mono<T> toRun, Function<Throwable, Mono<T>> fallback) {
                                return toRun;
                            }
                            @Override
                            public <T> Flux<T> run(Flux<T> toRun, Function<Throwable, Flux<T>> fallback) {
                                return toRun;
                            }
                        };
                    }
                    @Override
                    protected ConfigBuilder<Object> configBuilder(String id) { return null; }
                    @Override
                    public void configureDefault(Function<String, Object> defaultConfiguration) {}
                };

        ServiceClient serviceClient = new ServiceClient(webClientBuilder, circuitBreakerFactory);

        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_JWT_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", TEST_ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", TEST_REFRESH_EXPIRATION);
        jwtUtil.initKey();

        geoDataService = new GeoDataService(serviceClient, jwtUtil);
        // Remove trailing slash from baseUrl to match how the service builds URLs
        ReflectionTestUtils.setField(geoDataService, "geoServiceUrl", baseUrl.replaceAll("/$", ""));
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ── POST /geo/save ─────────────────────────────────────────

    @Test
    @DisplayName("saveWithOwnerEmail: should inject ownerEmail and return 202 response")
    void saveWithOwnerEmail_shouldInjectEmailAndForward() throws Exception {
        // Geospatial service returns 202 with job status
        String geoResponse = "{\"success\":true,\"data\":{\"trackingId\":\"abc-123\",\"status\":\"PENDING\"}}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(202)
                .setBody(geoResponse)
                .addHeader("Content-Type", "application/json"));

        String token = jwtUtil.generate(TEST_EMAIL, "USER", "ACCESS");
        String authorization = "Bearer " + token;

        ObjectNode body = objectMapper.createObjectNode();
        body.put("youtubeContentID", "yt-test-123");
        body.put("youtubeTitle", "Test Video");

        StepVerifier.create(geoDataService.saveWithOwnerEmail(authorization, body))
                .assertNext(result -> {
                    assertThat(result).contains("trackingId");
                    assertThat(result).contains("PENDING");
                })
                .verifyComplete();

        // Verify the request sent to geospatial-service has ownerEmail injected
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/geo/save");
        String sentBody = request.getBody().readUtf8();
        JsonNode sentJson = objectMapper.readTree(sentBody);
        assertThat(sentJson.get("ownerEmail").asText()).isEqualTo(TEST_EMAIL);
        assertThat(sentJson.get("youtubeContentID").asText()).isEqualTo("yt-test-123");
    }

    // ── GET /geo/status/{trackingId} ───────────────────────────

    @Test
    @DisplayName("getJobStatus: should return COMPLETED status")
    void getJobStatus_shouldReturnCompletedStatus() {
        String statusResponse = "{\"success\":true,\"data\":{\"trackingId\":\"abc-123\",\"status\":\"COMPLETED\",\"chaptersCount\":3,\"placesCount\":2}}";
        mockWebServer.enqueue(new MockResponse()
                .setBody(statusResponse)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(geoDataService.getJobStatus("abc-123"))
                .assertNext(result -> {
                    assertThat(result).contains("COMPLETED");
                    assertThat(result).contains("chaptersCount");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getJobStatus: should return DUPLICATE status")
    void getJobStatus_shouldReturnDuplicateStatus() {
        String statusResponse = "{\"success\":true,\"data\":{\"trackingId\":\"abc-456\",\"status\":\"DUPLICATE\"}}";
        mockWebServer.enqueue(new MockResponse()
                .setBody(statusResponse)
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(geoDataService.getJobStatus("abc-456"))
                .assertNext(result -> assertThat(result).contains("DUPLICATE"))
                .verifyComplete();
    }

    @Test
    @DisplayName("getJobStatus: should propagate 404 from geospatial service")
    void getJobStatus_shouldPropagate404() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"success\":false,\"errorCode\":\"JOB_NOT_FOUND\"}")
                .addHeader("Content-Type", "application/json"));

        StepVerifier.create(geoDataService.getJobStatus("unknown-id"))
                .expectError()
                .verify();
    }

    // ── DELETE /auth/geo/{youtubeContentId} ─────────────────────

    @Test
    @DisplayName("deleteByYoutubeContentId: should extract email and forward delete")
    void deleteByYoutubeContentId_shouldExtractEmailAndDelete() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"success\":true,\"data\":\"deleted\"}")
                .addHeader("Content-Type", "application/json"));

        String token = jwtUtil.generate(TEST_EMAIL, "USER", "ACCESS");
        String authorization = "Bearer " + token;

        StepVerifier.create(geoDataService.deleteByYoutubeContentId(authorization, "yt-test-123"))
                .assertNext(result -> assertThat(result).contains("deleted"))
                .verifyComplete();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("/geo/data/delete/adventuretubedata");
        assertThat(request.getPath()).contains("youtubeContentId=yt-test-123");
        assertThat(request.getPath()).contains("ownerEmail=" + TEST_EMAIL);
    }

    @Test
    @DisplayName("deleteByYoutubeContentId: should propagate 403 ownership mismatch")
    void deleteByYoutubeContentId_shouldPropagate403() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(403)
                .setBody("{\"success\":false,\"errorCode\":\"OWNERSHIP_MISMATCH\"}")
                .addHeader("Content-Type", "application/json"));

        String token = jwtUtil.generate(TEST_EMAIL, "USER", "ACCESS");

        StepVerifier.create(geoDataService.deleteByYoutubeContentId("Bearer " + token, "yt-other"))
                .expectError()
                .verify();
    }
}
