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
public class ChapterScreenShotService {
    private static final String BASE_URL = "http://GEOSPATIAL-SERVICE";
    private final ServiceClient serviceClient;

    public Mono<JsonNode> getScreenshotStatus(String youtubeContentId) {
        return serviceClient.getReactive(BASE_URL , "/geo/screenshot/" + youtubeContentId, new ParameterizedTypeReference<>() {});
    }
}
