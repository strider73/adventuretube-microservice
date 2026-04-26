package com.adventuretube.web.service;

import com.adventuretube.common.client.ServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoDataService {

    private static final String BASE_URL = "http://GEOSPATIAL-SERVICE";
    private final ServiceClient serviceClient;

    public Mono<JsonNode> findAll() {
        return serviceClient.<JsonNode>getReactive(BASE_URL, "/geo/data",
                new ParameterizedTypeReference<>() {});
    }

    public Mono<JsonNode> findById(String id) {
        return serviceClient.<JsonNode>getReactive(BASE_URL, "/geo/data/" + id,
                new ParameterizedTypeReference<>() {});
    }

    public Mono<JsonNode> findByYoutubeContentID(String youtubeContentID) {
        return serviceClient.<JsonNode>getReactive(BASE_URL, "/geo/data/youtube/" + youtubeContentID,
                new ParameterizedTypeReference<>() {});
    }

    public Mono<JsonNode> findByContentType(String contentType) {
        return serviceClient.<JsonNode>getReactive(BASE_URL, "/geo/data/type/" + contentType,
                new ParameterizedTypeReference<>() {});
    }

    public Mono<JsonNode> findByCategory(String category) {
        return serviceClient.<JsonNode>getReactive(BASE_URL, "/geo/data/category/" + category,
                new ParameterizedTypeReference<>() {});
    }

    public Mono<JsonNode> count() {
        return serviceClient.<JsonNode>getReactive(BASE_URL, "/geo/data/count",
                new ParameterizedTypeReference<>() {});
    }

    public Mono<JsonNode> findWithinBounds(double swLat, double swLng, double neLat, double neLng) {
        String path = String.format("/geo/data/bounds?swLat=%s&swLng=%s&neLat=%s&neLng=%s",
                swLat, swLng, neLat, neLng);
        return serviceClient.<JsonNode>getReactive(BASE_URL, path,
                new ParameterizedTypeReference<>() {});
    }

    public Mono<JsonNode> getScreenshotStatus(String youtubeContentId) {
        return serviceClient.<JsonNode>getReactive(BASE_URL, "/geo/data/screenshot-status/" +
                        youtubeContentId,
                new ParameterizedTypeReference<>() {});
    }
}
