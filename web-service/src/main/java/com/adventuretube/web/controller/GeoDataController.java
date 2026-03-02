package com.adventuretube.web.controller;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.web.service.GeoDataService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/web/geo")
@RequiredArgsConstructor
@Tag(name = "Web Controller")
public class GeoDataController {

    private final GeoDataService geoDataService;

    @GetMapping("/data")
    public ResponseEntity<ServiceResponse<JsonNode>> findAll() {
        return ResponseEntity.ok(wrapResponse("Geospatial data retrieved", geoDataService.findAll()));
    }

    @GetMapping("/data/{id}")
    public ResponseEntity<ServiceResponse<JsonNode>> findById(@PathVariable String id) {
        return ResponseEntity.ok(wrapResponse("Geospatial data retrieved", geoDataService.findById(id)));
    }

    @GetMapping("/data/youtube/{youtubeContentID}")
    public ResponseEntity<ServiceResponse<JsonNode>> findByYoutubeContentID(@PathVariable String youtubeContentID) {
        return ResponseEntity.ok(wrapResponse("Geospatial data retrieved", geoDataService.findByYoutubeContentID(youtubeContentID)));
    }

    @GetMapping("/data/type/{contentType}")
    public ResponseEntity<ServiceResponse<JsonNode>> findByContentType(@PathVariable String contentType) {
        return ResponseEntity.ok(wrapResponse("Geospatial data retrieved", geoDataService.findByContentType(contentType)));
    }

    @GetMapping("/data/category/{category}")
    public ResponseEntity<ServiceResponse<JsonNode>> findByCategory(@PathVariable String category) {
        return ResponseEntity.ok(wrapResponse("Geospatial data retrieved", geoDataService.findByCategory(category)));
    }

    @GetMapping("/data/count")
    public ResponseEntity<ServiceResponse<JsonNode>> count() {
        return ResponseEntity.ok(wrapResponse("Geospatial data count retrieved", geoDataService.count()));
    }

    private ServiceResponse<JsonNode> wrapResponse(String message, JsonNode data) {
        return ServiceResponse.<JsonNode>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
