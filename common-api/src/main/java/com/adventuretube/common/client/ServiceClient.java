package com.adventuretube.common.client;

import com.adventuretube.common.api.response.ServiceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

/**
 * Generic client for inter-service communication.
 *
 * Provides centralized error handling for all service-to-service calls:
 * - 4xx errors: Extracted from response body and signalled as ServiceClientException
 * - 5xx errors: Signalled as ServiceClientException with SERVER_ERROR
 * - Network failures: Signalled as ServiceClientException with SERVER_NOT_AVAILABLE
 *
 * Usage:
 * <pre>
 * {@code
 * // Reactive POST request to MEMBER-SERVICE
 * Mono<ServiceResponse<MemberDTO>> response = serviceClient.postReactive(
 *     "http://MEMBER-SERVICE",
 *     "/member/registerMember",
 *     memberDTO,
 *     new ParameterizedTypeReference<ServiceResponse<MemberDTO>>() {}
 * );
 *
 * // Reactive GET request to MEMBER-SERVICE
 * Mono<ServiceResponse<MemberDTO>> response = serviceClient.getReactive(
 *     "http://MEMBER-SERVICE",
 *     "/member/findMember",
 *     new ParameterizedTypeReference<ServiceResponse<MemberDTO>>() {}
 * );
 * }
 * </pre>
 */
@Slf4j
@Component
public class ServiceClient {

    private final WebClient.Builder webClientBuilder;

    public ServiceClient(@LoadBalanced WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Perform a non-blocking POST request to a target service.
     *
     * @param baseUrl      The service base URL (e.g., "http://MEMBER-SERVICE")
     * @param path         The endpoint path (e.g., "/member/registerMember")
     * @param body         The request body
     * @param responseType The expected response type
     * @param <T>          The type of data in ServiceResponse
     * @param <R>          The type of request body
     * @return Mono containing ServiceResponse with the result
     */
    public <T, R> Mono<ServiceResponse<T>> postReactive(String baseUrl, String path, R body,
                                                         ParameterizedTypeReference<ServiceResponse<T>> responseType) {
        WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();
        String serviceName = extractServiceName(baseUrl);

        return webClient.post()
                .uri(path)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(ServiceResponse.class)
                                .flatMap(errorResponse -> {
                                    log.error("{} 4xx error: {} - {}",
                                            serviceName, errorResponse.getErrorCode(), errorResponse.getMessage());
                                    return Mono.error(new ServiceClientException(
                                            serviceName,
                                            errorResponse.getErrorCode(),
                                            errorResponse.getMessage(),
                                            response.statusCode().value()
                                    ));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(ServiceResponse.class)
                                .<Throwable>flatMap(errorResponse -> {
                                    log.error("{} 5xx error: {} - {}",
                                            serviceName, errorResponse.getErrorCode(), errorResponse.getMessage());
                                    return Mono.error(new ServiceClientException(
                                            serviceName,
                                            errorResponse.getErrorCode(),
                                            errorResponse.getMessage(),
                                            response.statusCode().value()
                                    ));
                                })
                                .switchIfEmpty(Mono.error(new ServiceClientException(
                                        serviceName,
                                        "SERVER_ERROR",
                                        serviceName + " returned 5xx with no body",
                                        response.statusCode().value()
                                )))
                )
                .bodyToMono(responseType)
                .onErrorMap(WebClientRequestException.class, ex -> {
                    log.error("Network error calling {}: {}", serviceName, ex.getMessage());
                    return new ServiceClientException(serviceName, "SERVER_NOT_AVAILABLE",
                            serviceName + " is not available", 503);
                });
    }

    /**
     * Perform a non-blocking GET request to a target service.
     *
     * @param baseUrl      The service base URL (e.g., "http://MEMBER-SERVICE")
     * @param path         The endpoint path
     * @param responseType The expected response type
     * @param <T>          The type of data in ServiceResponse
     * @return Mono containing ServiceResponse with the result
     */
    public <T> Mono<ServiceResponse<T>> getReactive(String baseUrl, String path,
                                                     ParameterizedTypeReference<ServiceResponse<T>> responseType) {
        WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();
        String serviceName = extractServiceName(baseUrl);

        return webClient.get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(ServiceResponse.class)
                                .flatMap(errorResponse -> {
                                    log.error("{} 4xx error: {} - {}",
                                            serviceName, errorResponse.getErrorCode(), errorResponse.getMessage());
                                    return Mono.error(new ServiceClientException(
                                            serviceName,
                                            errorResponse.getErrorCode(),
                                            errorResponse.getMessage(),
                                            response.statusCode().value()
                                    ));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        response.bodyToMono(ServiceResponse.class)
                                .<Throwable>flatMap(errorResponse -> {
                                    log.error("{} 5xx error: {} - {}",
                                            serviceName, errorResponse.getErrorCode(), errorResponse.getMessage());
                                    return Mono.error(new ServiceClientException(
                                            serviceName,
                                            errorResponse.getErrorCode(),
                                            errorResponse.getMessage(),
                                            response.statusCode().value()
                                    ));
                                })
                                .switchIfEmpty(Mono.error(new ServiceClientException(
                                        serviceName,
                                        "SERVER_ERROR",
                                        serviceName + " returned 5xx with no body",
                                        response.statusCode().value()
                                )))
                )
                .bodyToMono(responseType)
                .onErrorMap(WebClientRequestException.class, ex -> {
                    log.error("Network error calling {}: {}", serviceName, ex.getMessage());
                    return new ServiceClientException(serviceName, "SERVER_NOT_AVAILABLE",
                            serviceName + " is not available", 503);
                });
    }

    /**
     * Extract service name from URL for logging.
     * e.g., "http://MEMBER-SERVICE" → "MEMBER-SERVICE"
     */
    private String extractServiceName(String baseUrl) {
        return baseUrl.replace("http://", "").replace("https://", "");
    }
}
