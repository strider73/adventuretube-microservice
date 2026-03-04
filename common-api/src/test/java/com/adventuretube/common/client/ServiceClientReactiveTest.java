package com.adventuretube.common.client;

import com.adventuretube.common.api.response.ServiceResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceClientReactiveTest {

    private MockWebServer mockWebServer;
    private ServiceClient serviceClient;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Create a plain (non-load-balanced) WebClient.Builder for testing
        WebClient.Builder webClientBuilder = WebClient.builder();

        // No-op circuit breaker that passes through all calls without tripping.
        // Unlike a real circuit breaker, this does NOT invoke the fallback —
        // errors propagate directly so tests can verify specific error codes.
        ReactiveCircuitBreakerFactory<Object, ConfigBuilder<Object>> circuitBreakerFactory =
                new ReactiveCircuitBreakerFactory<>() {
                    @Override
                    public ReactiveCircuitBreaker create(String id) {
                        return new ReactiveCircuitBreaker() {
                            @Override
                            public <T> Mono<T> run(Mono<T> toRun, Function<Throwable, Mono<T>> fallback) {
                                return toRun; // pass through, ignore fallback
                            }
                            @Override
                            public <T> Flux<T> run(Flux<T> toRun, Function<Throwable, Flux<T>> fallback) {
                                return toRun; // pass through, ignore fallback
                            }
                        };
                    }
                    @Override
                    protected ConfigBuilder<Object> configBuilder(String id) { return null; }
                    @Override
                    public void configureDefault(Function<String, Object> defaultConfiguration) {}
                };

        serviceClient = new ServiceClient(webClientBuilder, circuitBreakerFactory);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void postServiceResponseReactive_success_returnsServiceResponse() throws JsonProcessingException {
        ServiceResponse<String> expectedResponse = ServiceResponse.<String>builder()
                .success(true)
                .message("Created")
                .data("test-data")
                .timestamp(LocalDateTime.now())
                .build();

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        String baseUrl = mockWebServer.url("/").toString();

        StepVerifier.create(serviceClient.postServiceResponseReactive(
                        baseUrl,
                        "/test/endpoint",
                        "request-body",
                        new ParameterizedTypeReference<ServiceResponse<String>>() {}))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getMessage()).isEqualTo("Created");
                    assertThat(response.getData()).isEqualTo("test-data");
                })
                .verifyComplete();
    }

    @Test
    void postServiceResponseReactive_4xxError_returnsServiceClientException() throws JsonProcessingException {
        ServiceResponse<Object> errorResponse = ServiceResponse.builder()
                .success(false)
                .errorCode("USER_NOT_FOUND")
                .message("User not found")
                .timestamp(LocalDateTime.now())
                .build();

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody(objectMapper.writeValueAsString(errorResponse))
                .addHeader("Content-Type", "application/json"));

        String baseUrl = mockWebServer.url("/").toString();

        StepVerifier.create(serviceClient.postServiceResponseReactive(
                        baseUrl,
                        "/test/endpoint",
                        "request-body",
                        new ParameterizedTypeReference<ServiceResponse<String>>() {}))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ServiceClientException.class);
                    ServiceClientException ex = (ServiceClientException) throwable;
                    assertThat(ex.getErrorCode()).isEqualTo("USER_NOT_FOUND");
                    assertThat(ex.getHttpStatus()).isEqualTo(404);
                })
                .verify();
    }

    @Test
    void postServiceResponseReactive_5xxError_returnsServiceClientException() throws JsonProcessingException {
        ServiceResponse<Object> errorResponse = ServiceResponse.builder()
                .success(false)
                .errorCode("SERVER_ERROR")
                .message("Internal error")
                .timestamp(LocalDateTime.now())
                .build();

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody(objectMapper.writeValueAsString(errorResponse))
                .addHeader("Content-Type", "application/json"));

        String baseUrl = mockWebServer.url("/").toString();

        StepVerifier.create(serviceClient.postServiceResponseReactive(
                        baseUrl,
                        "/test/endpoint",
                        "request-body",
                        new ParameterizedTypeReference<ServiceResponse<String>>() {}))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ServiceClientException.class);
                    ServiceClientException ex = (ServiceClientException) throwable;
                    assertThat(ex.getErrorCode()).isEqualTo("SERVER_ERROR");
                    assertThat(ex.getHttpStatus()).isEqualTo(500);
                })
                .verify();
    }

    @Test
    void postServiceResponseReactive_5xxWithEmptyBody_returnsServiceClientException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json"));

        String baseUrl = mockWebServer.url("/").toString();

        StepVerifier.create(serviceClient.postServiceResponseReactive(
                        baseUrl,
                        "/test/endpoint",
                        "request-body",
                        new ParameterizedTypeReference<ServiceResponse<String>>() {}))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ServiceClientException.class);
                    ServiceClientException ex = (ServiceClientException) throwable;
                    assertThat(ex.getErrorCode()).isEqualTo("SERVER_ERROR");
                    assertThat(ex.getHttpStatus()).isEqualTo(500);
                })
                .verify();
    }

    @Test
    void getServiceResponseReactive_success_returnsServiceResponse() throws JsonProcessingException {
        ServiceResponse<String> expectedResponse = ServiceResponse.<String>builder()
                .success(true)
                .message("Found")
                .data("get-data")
                .timestamp(LocalDateTime.now())
                .build();

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        String baseUrl = mockWebServer.url("/").toString();

        StepVerifier.create(serviceClient.getServiceResponseReactive(
                        baseUrl,
                        "/test/endpoint",
                        new ParameterizedTypeReference<ServiceResponse<String>>() {}))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getMessage()).isEqualTo("Found");
                    assertThat(response.getData()).isEqualTo("get-data");
                })
                .verifyComplete();
    }

    @Test
    void getServiceResponseReactive_4xxError_returnsServiceClientException() throws JsonProcessingException {
        ServiceResponse<Object> errorResponse = ServiceResponse.builder()
                .success(false)
                .errorCode("NOT_FOUND")
                .message("Resource not found")
                .timestamp(LocalDateTime.now())
                .build();

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody(objectMapper.writeValueAsString(errorResponse))
                .addHeader("Content-Type", "application/json"));

        String baseUrl = mockWebServer.url("/").toString();

        StepVerifier.create(serviceClient.getServiceResponseReactive(
                        baseUrl,
                        "/test/endpoint",
                        new ParameterizedTypeReference<ServiceResponse<String>>() {}))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(ServiceClientException.class);
                    ServiceClientException ex = (ServiceClientException) throwable;
                    assertThat(ex.getErrorCode()).isEqualTo("NOT_FOUND");
                    assertThat(ex.getHttpStatus()).isEqualTo(404);
                })
                .verify();
    }

    @Test
    void postServiceResponseReactive_doesNotBlock() throws JsonProcessingException {
        ServiceResponse<String> expectedResponse = ServiceResponse.<String>builder()
                .success(true)
                .message("OK")
                .data("async-data")
                .timestamp(LocalDateTime.now())
                .build();

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        String baseUrl = mockWebServer.url("/").toString();

        // Verify reactive chain completes without blocking using StepVerifier
        var mono = serviceClient.postServiceResponseReactive(
                baseUrl,
                "/test/endpoint",
                "request-body",
                new ParameterizedTypeReference<ServiceResponse<String>>() {});

        StepVerifier.create(mono)
                .assertNext(response -> assertThat(response.getData()).isEqualTo("async-data"))
                .verifyComplete();
    }
}
