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
    public ResponseEntity<ServiceResponse<JsonNode>> findAll() {
        return ResponseEntity.ok(wrapResponse("Geospatial data retrieved", geoDataService.findAll()));
    }

    @Operation(summary = "Get geospatial data by ID")
    @ApiResponse(responseCode = "200", description = "Data retrieved.")
    @GetMapping("/data/{id}")
    public ResponseEntity<ServiceResponse<JsonNode>> findById(@PathVariable String id) {
        return ResponseEntity.ok(wrapResponse("Geospatial data retrieved", geoDataService.findById(id)));
    }

    @Operation(summary = "Get geospatial data by YouTube content ID")
    @ApiResponse(responseCode = "200", description = "Data retrieved.")
    @GetMapping("/data/youtube/{youtubeContentID}")
    public ResponseEntity<ServiceResponse<JsonNode>> findByYoutubeContentID(@PathVariable String youtubeContentID) {
        return ResponseEntity.ok(wrapResponse("Geospatial data retrieved", geoDataService.findByYoutubeContentID(youtubeContentID)));
    }

    @Operation(summary = "Get geospatial data by content type")
    @ApiResponse(responseCode = "200", description = "Data retrieved.")
    @GetMapping("/data/type/{contentType}")
    public ResponseEntity<ServiceResponse<JsonNode>> findByContentType(@PathVariable String contentType) {
        return ResponseEntity.ok(wrapResponse("Geospatial data retrieved", geoDataService.findByContentType(contentType)));
    }

    @Operation(summary = "Get geospatial data by category")
    @ApiResponse(responseCode = "200", description = "Data retrieved.")
    @GetMapping("/data/category/{category}")
    public ResponseEntity<ServiceResponse<JsonNode>> findByCategory(@PathVariable String category) {
        return ResponseEntity.ok(wrapResponse("Geospatial data retrieved", geoDataService.findByCategory(category)));
    }

    @Operation(summary = "Get geospatial data within bounding box")
    @ApiResponse(responseCode = "200", description = "Data within bounds retrieved.")
    @GetMapping("/data/bounds")
    public ResponseEntity<ServiceResponse<JsonNode>> findWithinBounds(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng) {
        return ResponseEntity.ok(wrapResponse("Geospatial data within bounds retrieved",
                geoDataService.findWithinBounds(swLat, swLng, neLat, neLng)));
    }

    @Operation(summary = "Get total count of geospatial data")
    @ApiResponse(responseCode = "200", description = "Count retrieved.")
    @GetMapping("/data/count")
    public ResponseEntity<ServiceResponse<JsonNode>> count() {
        return ResponseEntity.ok(wrapResponse("Geospatial data count retrieved", geoDataService.count()));
    }

    @GetMapping("/data/screenshot-status/{youtubeContentId}")
    public ResponseEntity<ServiceResponse<JsonNode>> getScreenshotStatus(@PathVariable String
                                                                                 youtubeContentId) {
        return ResponseEntity.ok(wrapResponse("Screenshot status retrieved",
                geoDataService.getScreenshotStatus(youtubeContentId)));
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
