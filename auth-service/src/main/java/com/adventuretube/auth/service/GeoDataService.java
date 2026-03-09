package com.adventuretube.auth.service;

import com.adventuretube.common.client.ServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class GeoDataService {

    @Value("${geospatial-service.url:http://GEOSPATIAL-SERVICE}")
    private String geoServiceUrl;

    private final ServiceClient serviceClient;
    private final JwtUtil jwtUtil;

    public GeoDataService(ServiceClient serviceClient, JwtUtil jwtUtil) {
        this.serviceClient = serviceClient;
        this.jwtUtil = jwtUtil;
    }

    public Mono<String> saveWithOwnerEmail(String authorization, JsonNode body) {
        return Mono.fromCallable(() -> {
                    String token = authorization.replace("Bearer ", "");
                    String email = jwtUtil.extractUsername(token);
                    ((ObjectNode) body).put("ownerEmail", email);
                    return body;
                })
                .flatMap(enrichedBody -> serviceClient.postReactive(
                        geoServiceUrl, "/geo/save", enrichedBody,
                        new ParameterizedTypeReference<String>() {}));
    }

    public Flux<ServerSentEvent<String>> streamJobStatus(String trackingId) {
        return serviceClient.getSseStreamReactive(
                geoServiceUrl,
                "/geo/status/stream/" + trackingId);
    }

    public Mono<String> getJobStatus(String trackingId) {
        return serviceClient.getReactive(
                geoServiceUrl,
                "/geo/status/" + trackingId,
                new ParameterizedTypeReference<String>() {});
    }

    public Mono<String> deleteByYoutubeContentId(String authorization, String youtubeContentId) {
        return Mono.fromCallable(() -> {
                    String token = authorization.replace("Bearer ", "");
                    return jwtUtil.extractUsername(token);
                })
                .flatMap(ownerEmail -> serviceClient.deleteReactive(
                        geoServiceUrl,
                        "/geo/data/delete/adventuretubedata?youtubeContentId=" + youtubeContentId + "&ownerEmail=" + ownerEmail,
                        new ParameterizedTypeReference<String>() {}));
    }
}
