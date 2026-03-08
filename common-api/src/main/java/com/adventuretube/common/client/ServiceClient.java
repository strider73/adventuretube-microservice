package com.adventuretube.common.client;

import com.adventuretube.common.api.response.ServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Generic client for inter-service communication via WebClient.
 *
 * <p>All methods are built on reactive WebClient. Blocking services (Tomcat + virtual threads)
 * use the {@code *NonReactive()} convenience wrappers which call {@code .block()}.
 *
 * <h3>Method naming convention</h3>
 * <pre>
 *   {http-method} + {Raw|ServiceResponse} + {Reactive|NonReactive}
 *
 *   Raw             = target returns a plain entity (e.g., String, AdventureTubeData)
 *   ServiceResponse = target returns ServiceResponse&lt;T&gt; wrapper
 *   Reactive        = returns Mono&lt;T&gt;  (for Netty / reactive callers)
 *   NonReactive     = calls .block()    (for Tomcat / blocking callers)
 * </pre>
 *
 * <h3>Error handling</h3>
 * <ul>
 *   <li>4xx errors: {@link ServiceClientException} with isClientError()=true (passed through circuit breaker)</li>
 *   <li>5xx errors: {@link ServiceClientException} with isServerError()=true</li>
 *   <li>Network failures: {@link ServiceClientException} with SERVER_NOT_AVAILABLE</li>
 *   <li>Circuit breaker open: {@link ServiceClientException} with CIRCUIT_OPEN</li>
 * </ul>
 *
 * <h3>Usage examples</h3>
 * <pre>{@code
 * // Reactive (auth-service on Netty)
 * Mono<ServiceResponse<MemberDTO>> result = serviceClient.postServiceResponseReactive(
 *     "http://MEMBER-SERVICE", "/member/register", dto,
 *     new ParameterizedTypeReference<ServiceResponse<MemberDTO>>() {});
 *
 * // Blocking (web-service on Tomcat + virtual threads)
 * ServiceResponse<MemberDTO> result = serviceClient.postServiceResponseNonReactive(
 *     "http://MEMBER-SERVICE", "/member/register", dto,
 *     new ParameterizedTypeReference<ServiceResponse<MemberDTO>>() {});
 *
 * // Raw reactive (auth-service -> geospatial-service)
 * Mono<String> result = serviceClient.postRawReactive(
 *     "http://GEOSPATIAL-SERVICE", "/geo/save", jsonBody,
 *     new ParameterizedTypeReference<String>() {});
 * }</pre>
 */
@Slf4j
@Component
public class ServiceClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient.Builder webClientBuilder;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;

    public ServiceClient(@LoadBalanced WebClient.Builder webClientBuilder,
                         ReactiveCircuitBreakerFactory circuitBreakerFactory) {
        this.webClientBuilder = webClientBuilder;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    // ════════════════════════════════════════════════════════════════════
    //  1. ServiceResponse Reactive — target returns ServiceResponse<T>
    // ════════════════════════════════════════════════════════════════════

    /** Non-blocking POST expecting {@code ServiceResponse<T>}. */
    public <T, R> Mono<ServiceResponse<T>> postServiceResponseReactive(
            String baseUrl, String path, R body,
            ParameterizedTypeReference<ServiceResponse<T>> responseType) {

        String serviceName = extractServiceName(baseUrl);
        Mono<ServiceResponse<T>> call = webClientBuilder.baseUrl(baseUrl).build()
                .post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> handleErrorServiceResponse(serviceName, response))
                .bodyToMono(responseType)
                .timeout(DEFAULT_TIMEOUT)
                .onErrorMap(WebClientRequestException.class, ex -> networkError(serviceName, ex))
                .onErrorMap(java.util.concurrent.TimeoutException.class, ex -> timeoutError(serviceName, path, ex));

        return withCircuitBreaker(serviceName, call);
    }

    /** Non-blocking GET expecting {@code ServiceResponse<T>}. */
    public <T> Mono<ServiceResponse<T>> getServiceResponseReactive(
            String baseUrl, String path,
            ParameterizedTypeReference<ServiceResponse<T>> responseType) {

        String serviceName = extractServiceName(baseUrl);
        Mono<ServiceResponse<T>> call = webClientBuilder.baseUrl(baseUrl).build()
                .get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> handleErrorServiceResponse(serviceName, response))
                .bodyToMono(responseType)
                .timeout(DEFAULT_TIMEOUT)
                .onErrorMap(WebClientRequestException.class, ex -> networkError(serviceName, ex))
                .onErrorMap(java.util.concurrent.TimeoutException.class, ex -> timeoutError(serviceName, path, ex));

        return withCircuitBreaker(serviceName, call);
    }

    // ════════════════════════════════════════════════════════════════════
    //  2. Raw Reactive — target returns plain entity (not ServiceResponse)
    // ════════════════════════════════════════════════════════════════════

    /** Non-blocking GET returning a raw entity. */
    public <T> Mono<T> getRawReactive(String baseUrl, String path,
                                      ParameterizedTypeReference<T> responseType) {

        String serviceName = extractServiceName(baseUrl);
        Mono<T> call = webClientBuilder.baseUrl(baseUrl).build()
                .get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> handleErrorRaw(serviceName, "GET", path, response))
                .bodyToMono(responseType)
                .timeout(DEFAULT_TIMEOUT)
                .onErrorMap(WebClientRequestException.class, ex -> networkError(serviceName, ex))
                .onErrorMap(java.util.concurrent.TimeoutException.class, ex -> timeoutError(serviceName, path, ex));

        return withCircuitBreaker(serviceName, call);
    }

    /** Non-blocking POST returning a raw entity. */
    public <T, R> Mono<T> postRawReactive(String baseUrl, String path, R body,
                                           ParameterizedTypeReference<T> responseType) {

        String serviceName = extractServiceName(baseUrl);
        Mono<T> call = webClientBuilder.baseUrl(baseUrl).build()
                .post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> handleErrorRaw(serviceName, "POST", path, response))
                .bodyToMono(responseType)
                .timeout(DEFAULT_TIMEOUT)
                .onErrorMap(WebClientRequestException.class, ex -> networkError(serviceName, ex))
                .onErrorMap(java.util.concurrent.TimeoutException.class, ex -> timeoutError(serviceName, path, ex));

        return withCircuitBreaker(serviceName, call);
    }

    /** Non-blocking PUT returning a raw entity. */
    public <T, R> Mono<T> putRawReactive(String baseUrl, String path, R body,
                                          ParameterizedTypeReference<T> responseType) {

        String serviceName = extractServiceName(baseUrl);
        Mono<T> call = webClientBuilder.baseUrl(baseUrl).build()
                .put()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> handleErrorRaw(serviceName, "PUT", path, response))
                .bodyToMono(responseType)
                .timeout(DEFAULT_TIMEOUT)
                .onErrorMap(WebClientRequestException.class, ex -> networkError(serviceName, ex))
                .onErrorMap(java.util.concurrent.TimeoutException.class, ex -> timeoutError(serviceName, path, ex));

        return withCircuitBreaker(serviceName, call);
    }

    /** Non-blocking DELETE returning a raw entity. */
    public <T> Mono<T> deleteRawReactive(String baseUrl, String path,
                                          ParameterizedTypeReference<T> responseType) {

        String serviceName = extractServiceName(baseUrl);
        Mono<T> call = webClientBuilder.baseUrl(baseUrl).build()
                .delete()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> handleErrorRaw(serviceName, "DELETE", path, response))
                .bodyToMono(responseType)
                .timeout(DEFAULT_TIMEOUT)
                .onErrorMap(WebClientRequestException.class, ex -> networkError(serviceName, ex))
                .onErrorMap(java.util.concurrent.TimeoutException.class, ex -> timeoutError(serviceName, path, ex));

        return withCircuitBreaker(serviceName, call);
    }

    // ════════════════════════════════════════════════════════════════════
    //  3. NonReactive (blocking) — convenience wrappers calling .block()
    //     Safe on Tomcat + Java 21 virtual threads.
    // ════════════════════════════════════════════════════════════════════

    /** Blocking POST expecting {@code ServiceResponse<T>}. */
    public <T, R> ServiceResponse<T> postServiceResponseNonReactive(
            String baseUrl, String path, R body,
            ParameterizedTypeReference<ServiceResponse<T>> responseType) {
        return postServiceResponseReactive(baseUrl, path, body, responseType).block();
    }

    /** Blocking GET expecting {@code ServiceResponse<T>}. */
    public <T> ServiceResponse<T> getServiceResponseNonReactive(
            String baseUrl, String path,
            ParameterizedTypeReference<ServiceResponse<T>> responseType) {
        return getServiceResponseReactive(baseUrl, path, responseType).block();
    }

    /** Blocking GET returning a raw entity. */
    public <T> T getRawNonReactive(String baseUrl, String path,
                                   ParameterizedTypeReference<T> responseType) {
        return getRawReactive(baseUrl, path, responseType).block();
    }

    /** Blocking POST returning a raw entity. */
    public <T, R> T postRawNonReactive(String baseUrl, String path, R body,
                                        ParameterizedTypeReference<T> responseType) {
        return postRawReactive(baseUrl, path, body, responseType).block();
    }

    /** Blocking PUT returning a raw entity. */
    public <T, R> T putRawNonReactive(String baseUrl, String path, R body,
                                       ParameterizedTypeReference<T> responseType) {
        return putRawReactive(baseUrl, path, body, responseType).block();
    }

    /** Blocking DELETE returning a raw entity. */
    public <T> T deleteRawNonReactive(String baseUrl, String path,
                                       ParameterizedTypeReference<T> responseType) {
        return deleteRawReactive(baseUrl, path, responseType).block();
    }

    // ════════════════════════════════════════════════════════════════════
    //  4. Private helpers — shared error handling & circuit breaker
    // ════════════════════════════════════════════════════════════════════

    /**
     * Error handler for ServiceResponse endpoints.
     * Extracts errorCode and message from the ServiceResponse error body.
     * Falls back to a generic error if the body is empty.
     */
    private Mono<Throwable> handleErrorServiceResponse(
            String serviceName,
            org.springframework.web.reactive.function.client.ClientResponse response) {
        int status = response.statusCode().value();
        return response.bodyToMono(ServiceResponse.class)
                .<Throwable>flatMap(errorResponse -> {
                    log.error("{} {} error: {} - {}",
                            serviceName, status, errorResponse.getErrorCode(), errorResponse.getMessage());
                    return Mono.error(new ServiceClientException(
                            serviceName,
                            errorResponse.getErrorCode(),
                            errorResponse.getMessage(),
                            status));
                })
                .switchIfEmpty(Mono.error(new ServiceClientException(
                        serviceName,
                        status >= 500 ? "SERVER_ERROR" : "CLIENT_ERROR",
                        serviceName + " returned " + status + " with no body",
                        status)));
    }

    /**
     * Error handler for raw endpoints.
     * Error responses are always ServiceResponse (built by GlobalExceptionHandler on the target service),
     * even though success responses are plain entities.
     */
    private Mono<Throwable> handleErrorRaw(
            String serviceName, String method, String path,
            org.springframework.web.reactive.function.client.ClientResponse response) {
        int status = response.statusCode().value();
        return response.bodyToMono(ServiceResponse.class)
                .<Throwable>flatMap(errorResponse -> {
                    log.error("{} {} error on {} {}: {} - {}",
                            serviceName, status, method, path,
                            errorResponse.getErrorCode(), errorResponse.getMessage());
                    return Mono.error(new ServiceClientException(
                            serviceName,
                            errorResponse.getErrorCode(),
                            errorResponse.getMessage(),
                            status));
                })
                .switchIfEmpty(Mono.error(new ServiceClientException(
                        serviceName,
                        status >= 500 ? "SERVER_ERROR" : "CLIENT_ERROR",
                        serviceName + " returned " + status + " with no body",
                        status)));
    }

    /** Wraps a call with circuit breaker, passing through 4xx errors. */
    private <T> Mono<T> withCircuitBreaker(String serviceName, Mono<T> call) {
        ReactiveCircuitBreaker circuitBreaker = circuitBreakerFactory.create(serviceName);
        return circuitBreaker.run(call, throwable -> {
            if (throwable instanceof ServiceClientException sce && sce.isClientError()) {
                return Mono.error(throwable);
            }
            log.error("Circuit breaker open for {}: {}", serviceName, throwable.getMessage());
            return Mono.error(new ServiceClientException(
                    serviceName, "CIRCUIT_OPEN",
                    serviceName + " circuit breaker is open", 503));
        });
    }

    private ServiceClientException networkError(String serviceName, WebClientRequestException ex) {
        log.error("Network error calling {}: {}", serviceName, ex.getMessage());
        return new ServiceClientException(serviceName, "SERVER_NOT_AVAILABLE",
                serviceName + " is not available", 503);
    }

    private ServiceClientException timeoutError(String serviceName, String path, java.util.concurrent.TimeoutException ex) {
        log.error("Timeout calling {}{}: {}", serviceName, path, ex.getMessage());
        return new ServiceClientException(serviceName, "SERVER_NOT_AVAILABLE",
                serviceName + " timed out", 503);
    }

    /** Extract service name from URL for logging (e.g., "http://MEMBER-SERVICE" -> "MEMBER-SERVICE"). */
    private String extractServiceName(String baseUrl) {
        return baseUrl.replace("http://", "").replace("https://", "");
    }
}