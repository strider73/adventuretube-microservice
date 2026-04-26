package com.adventuretube.web.controller;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.web.service.GeoDataService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/web/geo")
@RequiredArgsConstructor
@Tag(name = "Web Geo Data Controller", description = "Public-facing geospatial data endpoints. Proxies requests to geospatial-service via ServiceClient.")
public class GeoDataController {

    private final GeoDataService geoDataService;

    @Operation(summary = "Get all geospatial data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All geospatial data retrieved."),
            @ApiResponse(responseCode = "503", description = "Geospatial service unavailable.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Service call failed",
                                      "errorCode": "SERVICE_UNAVAILABLE",
                                      "data": "GEOSPATIAL-SERVICE : web-service",
                                      "timestamp": "2026-03-23T14:00:00"
                                    }
                                    """)))
    })
    @GetMapping("/data")
    public Mono<ResponseEntity<ServiceResponse<JsonNode>>> findAll() {
        return geoDataService.findAll()
                        .map(data ->ResponseEntity.ok(wrapResponse("Geospatial data retrieved",data)));
    }

    @Operation(summary = "Get geospatial data by ID")
    @ApiResponse(responseCode = "200", description = "Data retrieved.")
    @GetMapping("/data/{id}")
    public Mono<ResponseEntity<ServiceResponse<JsonNode>>> findById(@PathVariable String id) {
        return geoDataService.findById(id)
                .map(data -> ResponseEntity.ok(wrapResponse("Geospatial data retrieved", data)));
    }

    @Operation(summary = "Get geospatial data by YouTube content ID")
    @ApiResponse(responseCode = "200", description = "Data retrieved.")
    @GetMapping("/data/youtube/{youtubeContentID}")
    public Mono<ResponseEntity<ServiceResponse<JsonNode>>> findByYoutubeContentID(@PathVariable String youtubeContentID) {
        return geoDataService.findByYoutubeContentID(youtubeContentID)
                .map(data -> ResponseEntity.ok(wrapResponse("Geospatial data retrieved", data)));
    }

    @Operation(summary = "Get geospatial data by content type")
    @ApiResponse(responseCode = "200", description = "Data retrieved.")
    @GetMapping("/data/type/{contentType}")
    public Mono<ResponseEntity<ServiceResponse<JsonNode>>> findByContentType(@PathVariable String contentType) {
        return geoDataService.findByContentType(contentType)
                .map(data -> ResponseEntity.ok(wrapResponse("Geospatial data retrieved", data)));
    }

    @Operation(summary = "Get geospatial data by category")
    @ApiResponse(responseCode = "200", description = "Data retrieved.")
    @GetMapping("/data/category/{category}")
    public Mono<ResponseEntity<ServiceResponse<JsonNode>>> findByCategory(@PathVariable String category) {
        return geoDataService.findByCategory(category)
                .map(data -> ResponseEntity.ok(wrapResponse("Geospatial data retrieved", data)));
    }

    @Operation(summary = "Get geospatial data within bounding box")
    @ApiResponse(responseCode = "200", description = "Data within bounds retrieved.")
    @GetMapping("/data/bounds")
    public Mono<ResponseEntity<ServiceResponse<JsonNode>>> findWithinBounds(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng) {
        return geoDataService.findWithinBounds(swLat, swLng, neLat, neLng)
                .map(data -> ResponseEntity.ok(wrapResponse("Geospatial data within bounds retrieved", data)));
    }

    @Operation(summary = "Get total count of geospatial data")
    @ApiResponse(responseCode = "200", description = "Count retrieved.")
    @GetMapping("/data/count")
    public Mono<ResponseEntity<ServiceResponse<JsonNode>>> count() {
        return geoDataService.count()
                .map(data -> ResponseEntity.ok(wrapResponse("Geospatial data count retrieved", data)));
    }

    @GetMapping("/data/screenshot-status/{youtubeContentId}")
    public Mono<ResponseEntity<ServiceResponse<JsonNode>>> getScreenshotStatus(@PathVariable String youtubeContentId) {
        return geoDataService.getScreenshotStatus(youtubeContentId)
                .map(data -> ResponseEntity.ok(wrapResponse("Screenshot status retrieved", data)));
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
