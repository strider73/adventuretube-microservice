package com.adventuretube.common.client;

import com.adventuretube.common.api.response.ServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Generic client for inter-service communication via WebClient.
 *
 * <p>All methods are built on reactive WebClient. Blocking services (Tomcat + virtual threads)
 * call {@code .block()} at the call site or use the reactive {@code Mono} directly.
 *
 * <h3>Method naming convention</h3>
 * <pre>
 *   {http-method} + Reactive
 *
 *   Reactive = returns Mono&lt;T&gt;
 *   Callers on Tomcat call .block() at the call site.
 *   Callers on Netty chain the Mono directly.
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
 * Mono<ServiceResponse<MemberDTO>> result = serviceClient.postReactive(
 *     "http://MEMBER-SERVICE", "/member/register", dto,
 *     new ParameterizedTypeReference<ServiceResponse<MemberDTO>>() {});
 *
 * // Blocking (web-service on Tomcat + virtual threads)
 * ServiceResponse<MemberDTO> result = serviceClient.postReactive(
 *     "http://MEMBER-SERVICE", "/member/register", dto,
 *     new ParameterizedTypeReference<ServiceResponse<MemberDTO>>() {}).block();
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
    //  Public API — reactive methods (callers .block() if needed)
    // ════════════════════════════════════════════════════════════════════

    /** Non-blocking POST. */
    public <T, R> Mono<T> postReactive(String baseUrl, String path, R body,
                                        ParameterizedTypeReference<T> responseType) {
        String serviceName = extractServiceName(baseUrl);
        Mono<T> call = webClientBuilder.baseUrl(baseUrl).build()
                .post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> handleError(serviceName, response))
                .bodyToMono(responseType)
                .timeout(DEFAULT_TIMEOUT)
                .onErrorMap(WebClientRequestException.class, ex -> networkError(serviceName, ex))
                .onErrorMap(TimeoutException.class, ex -> timeoutError(serviceName, path, ex));

        return withCircuitBreaker(serviceName, call);
    }

    /** Non-blocking GET. */
    public <T> Mono<T> getReactive(String baseUrl, String path,
                                    ParameterizedTypeReference<T> responseType) {
        String serviceName = extractServiceName(baseUrl);
        Mono<T> call = webClientBuilder.baseUrl(baseUrl).build()
                .get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> handleError(serviceName, response))
                .bodyToMono(responseType)
                .timeout(DEFAULT_TIMEOUT)
                .onErrorMap(WebClientRequestException.class, ex -> networkError(serviceName, ex))
                .onErrorMap(TimeoutException.class, ex -> timeoutError(serviceName, path, ex));

        return withCircuitBreaker(serviceName, call);
    }

    /** Non-blocking PUT. */
    public <T, R> Mono<T> putReactive(String baseUrl, String path, R body,
                                       ParameterizedTypeReference<T> responseType) {
        String serviceName = extractServiceName(baseUrl);
        Mono<T> call = webClientBuilder.baseUrl(baseUrl).build()
                .put()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> handleError(serviceName, response))
                .bodyToMono(responseType)
                .timeout(DEFAULT_TIMEOUT)
                .onErrorMap(WebClientRequestException.class, ex -> networkError(serviceName, ex))
                .onErrorMap(TimeoutException.class, ex -> timeoutError(serviceName, path, ex));

        return withCircuitBreaker(serviceName, call);
    }

    /** Non-blocking DELETE. */
    public <T> Mono<T> deleteReactive(String baseUrl, String path,
                                       ParameterizedTypeReference<T> responseType) {
        String serviceName = extractServiceName(baseUrl);
        Mono<T> call = webClientBuilder.baseUrl(baseUrl).build()
                .delete()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> handleError(serviceName, response))
                .bodyToMono(responseType)
                .timeout(DEFAULT_TIMEOUT)
                .onErrorMap(WebClientRequestException.class, ex -> networkError(serviceName, ex))
                .onErrorMap(TimeoutException.class, ex -> timeoutError(serviceName, path, ex));

        return withCircuitBreaker(serviceName, call);
    }

    /**
     * Non-blocking SSE stream. Returns Flux of ServerSentEvent.
     * Used by auth-service to proxy SSE streams from downstream services.
     * No circuit breaker — CB is designed for request/response, not long-lived streams.
     */
    public Flux<ServerSentEvent<String>> getSseStreamReactive(String baseUrl, String path) {
        String serviceName = extractServiceName(baseUrl);
        return webClientBuilder.baseUrl(baseUrl).build()
                .get()
                .uri(path)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> handleError(serviceName, response))
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .timeout(Duration.ofSeconds(35))
                .onErrorMap(WebClientRequestException.class, ex -> networkError(serviceName, ex))
                .onErrorMap(TimeoutException.class, ex -> timeoutError(serviceName, path, ex));
    }

    // ════════════════════════════════════════════════════════════════════
    //  Private helpers — error handling & circuit breaker
    // ════════════════════════════════════════════════════════════════════

    /**
     * Unified error handler for all endpoints.
     * Error responses are always {@link ServiceResponse} (built by GlobalExceptionHandler on the target service).
     * Falls back to a generic error if the body is empty.
     */
    private Mono<Throwable> handleError(String serviceName, ClientResponse response) {
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

    private ServiceClientException timeoutError(String serviceName, String path, TimeoutException ex) {
        log.error("Timeout calling {}{}: {}", serviceName, path, ex.getMessage());
        return new ServiceClientException(serviceName, "SERVER_NOT_AVAILABLE",
                serviceName + " timed out", 503);
    }

    /** Extract service name from URL for logging (e.g., "http://MEMBER-SERVICE" -> "MEMBER-SERVICE"). */
    private String extractServiceName(String baseUrl) {
        return baseUrl.replace("http://", "").replace("https://", "");
    }
}
