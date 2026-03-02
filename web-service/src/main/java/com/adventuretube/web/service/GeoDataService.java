package com.adventuretube.web.service;

import com.adventuretube.common.client.ServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoDataService {

    private static final String BASE_URL = "http://GEOSPATIAL-SERVICE";
    private final ServiceClient serviceClient;

    public JsonNode findAll() {
        return serviceClient.getRaw(BASE_URL, "/geo/data",
                new ParameterizedTypeReference<>() {});
    }

    public JsonNode findById(String id) {
        return serviceClient.getRaw(BASE_URL, "/geo/data/" + id,
                new ParameterizedTypeReference<>() {});
    }

    public JsonNode findByYoutubeContentID(String youtubeContentID) {
        return serviceClient.getRaw(BASE_URL, "/geo/data/youtube/" + youtubeContentID,
                new ParameterizedTypeReference<>() {});
    }

    public JsonNode findByContentType(String contentType) {
        return serviceClient.getRaw(BASE_URL, "/geo/data/type/" + contentType,
                new ParameterizedTypeReference<>() {});
    }

    public JsonNode findByCategory(String category) {
        return serviceClient.getRaw(BASE_URL, "/geo/data/category/" + category,
                new ParameterizedTypeReference<>() {});
    }

    public JsonNode count() {
        return serviceClient.getRaw(BASE_URL, "/geo/data/count",
                new ParameterizedTypeReference<>() {});
    }
}
