package com.adventuretube.auth.controller;

import com.adventuretube.auth.service.GeoDataService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/auth/geo")
@RequiredArgsConstructor
public class GeoDataController {

    private final GeoDataService geoDataService;

    @PostMapping("/save")
    public Mono<ResponseEntity<String>> saveAdventureTubeData(
            @RequestHeader("Authorization") String authorization,
            @RequestBody JsonNode body) {
        log.info("POST /auth/geo/save received");
        return geoDataService.saveWithOwnerEmail(authorization, body)
                .map(result -> ResponseEntity.accepted().body(result));
    }

    @GetMapping(value = "/status/stream/{trackingId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamJobStatus(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String trackingId) {
        log.info("SSE proxy /auth/geo/status/stream/{} requested", trackingId);
        return geoDataService.streamJobStatus(trackingId);
    }

    @GetMapping("/status/{trackingId}")
    public Mono<ResponseEntity<String>> getJobStatus(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String trackingId) {
        log.info("GET /auth/geo/status/{} requested", trackingId);
        return geoDataService.getJobStatus(trackingId)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{youtubeContentId}")
    public Mono<ResponseEntity<String>> deleteAdventureTubeData(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String youtubeContentId) {
        log.info("DELETE /auth/geo/{} requested", youtubeContentId);
        return geoDataService.deleteByYoutubeContentId(authorization, youtubeContentId)
                .map(result -> ResponseEntity.ok().body(result));
    }
}
