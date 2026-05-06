package com.adventuretube.auth.controller;

import com.adventuretube.auth.model.response.jobstatus.StoryJobStatusResponse;
import com.adventuretube.auth.service.GeoDataService;
import com.adventuretube.common.api.response.ServiceResponse;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/auth/geo")
@RequiredArgsConstructor
@Tag(name = "Geo (authenticated)", description = "Authenticated proxy endpoints for geospatial story operations. " +
        "Writes inject the caller's ownerEmail (from JWT) before forwarding to the geospatial service. " +
        "Reads forward downstream responses as opaque payloads.")
public class GeoDataController {

    private final GeoDataService geoDataService;

    // =========================
    // Save (async write) Endpoint
    // =========================
    @Operation(
            summary = "Save (publish) an AdventureTube story — asynchronous",
            description = "Forwards the request body to the geospatial service after enriching it with " +
                    "the authenticated user's email (extracted from the Bearer token). The downstream service " +
                    "writes synchronously and returns a tracking ID; the iOS client should subsequently open " +
                    "an SSE stream on /auth/geo/status/stream/{trackingId} to observe job progress.",
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Bearer JWT issued by /auth/token. Example: Bearer eyJhbGciOiJIUzI1NiJ9...",
                            schema = @Schema(type = "string")
                    )
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = JsonNode.class),
                            examples = @ExampleObject(
                                    name = "Save Story Example",
                                    value = """
                                            {
                                              "youtubeContentID": "abc123XYZ",
                                              "title": "Mt. Cook Day Hike",
                                              "category": "ADVENTURE",
                                              "chapters": [
                                                {
                                                  "youtubeTime": 0,
                                                  "category": "START",
                                                  "place": {
                                                    "placeID": "ChIJ...",
                                                    "name": "Aoraki / Mount Cook",
                                                    "latitude": -43.5950,
                                                    "longitude": 170.1418
                                                  }
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Accepted — story persisted synchronously, screenshot job queued. " +
                            "Response is ServiceResponse<StoryJobStatusView> containing trackingId and initial PENDING status.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Story saved successfully",
                                      "data": {
                                        "trackingId": "3f2c9b1e-7a4d-4f1c-9e2b-9b6a1d2c3e4f",
                                        "status": "PENDING",
                                        "youtubeContentID": "abc123XYZ"
                                      },
                                      "timestamp": "2026-05-04T08:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — missing, malformed, or expired JWT.",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error — unexpected failure while enriching or forwarding.",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service Unavailable — geospatial service unreachable or circuit breaker open.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping("/save")
    public Mono<ResponseEntity<ServiceResponse<StoryJobStatusResponse>>> saveAdventureTubeData(
            @RequestHeader("Authorization") String authorization,
            @RequestBody JsonNode body) {
        log.info("POST /auth/geo/save received");
        return geoDataService.saveWithOwnerEmail(authorization, body)
                .map(result ->
                        ResponseEntity.accepted().body(ServiceResponse.<StoryJobStatusResponse>builder()
                                .success(true)
                                .message("Story saved successfully")
                                .data(result)
                                .timestamp(java.time.LocalDateTime.now())
                                .build()));
    }

    // =========================
    // SSE Stream Endpoint
    // =========================
    @Operation(
            summary = "Stream job status updates over Server-Sent Events",
            description = "Opens a long-lived SSE connection that proxies job-status events from the geospatial " +
                    "service. Events typically progress PENDING → PROCESSING → COMPLETED / FAILED / DUPLICATED. " +
                    "Each event is a JSON-encoded JobStatusDTO delivered as `data:` lines per the SSE spec.",
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Bearer JWT issued by /auth/token.",
                            schema = @Schema(type = "string")
                    ),
                    @Parameter(
                            name = "trackingId",
                            in = ParameterIn.PATH,
                            required = true,
                            description = "Tracking ID returned by POST /auth/geo/save.",
                            schema = @Schema(type = "string", example = "trk_3f2c9b1e-7a4d-4f1c-9e2b-9b6a1d2c3e4f")
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "SSE stream opened. The connection remains open until a terminal event " +
                            "(COMPLETED/FAILED/DUPLICATED) is delivered or either side closes.",
                    content = @Content(
                            mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                            examples = @ExampleObject(value = """
                                    data: {"trackingId":"trk_3f2c...","status":"PENDING","message":null}

                                    data: {"trackingId":"trk_3f2c...","status":"PROCESSING","message":"Generating screenshots"}

                                    data: {"trackingId":"trk_3f2c...","status":"COMPLETED","message":"All screenshots uploaded"}
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — missing, malformed, or expired JWT.",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service Unavailable — geospatial service unreachable.",
                    content = @Content(mediaType = "application/json")
            )
    })
    //No ServiceResponse<T> wrapping for the streaming response.
    @GetMapping(value = "/status/stream/{trackingId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StoryJobStatusResponse>> streamJobStatus(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String trackingId) {
        log.info("SSE proxy /auth/geo/status/stream/{} requested", trackingId);
        return geoDataService.streamJobStatus(trackingId);
    }

    // =========================
    // Polling Status Endpoint
    // =========================
    @Operation(
            summary = "Get current job status (polling fallback)",
            description = "Returns the latest known JobStatusDTO for the given trackingId as a JSON string body. " +
                    "Used by iOS as a fallback when the SSE connection cannot be established.",
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Bearer JWT issued by /auth/token.",
                            schema = @Schema(type = "string")
                    ),
                    @Parameter(
                            name = "trackingId",
                            in = ParameterIn.PATH,
                            required = true,
                            description = "Tracking ID returned by POST /auth/geo/save.",
                            schema = @Schema(type = "string", example = "trk_3f2c9b1e-7a4d-4f1c-9e2b-9b6a1d2c3e4f")
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Current job status. Body is a JSON-encoded JobStatusDTO returned as a string.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "trackingId": "trk_3f2c9b1e-7a4d-4f1c-9e2b-9b6a1d2c3e4f",
                                      "status": "PROCESSING",
                                      "message": "Generating screenshots",
                                      "updatedAt": "2026-05-01T03:14:15Z"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — missing, malformed, or expired JWT.",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found — no job exists for the given trackingId.",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service Unavailable — geospatial service unreachable.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/status/{trackingId}")
    public Mono<ResponseEntity<ServiceResponse<StoryJobStatusResponse>>> getJobStatus(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String trackingId) {
        log.info("GET /auth/geo/status/{} requested", trackingId);
        return geoDataService.getJobStatus(trackingId)
                .map(result -> ResponseEntity.ok(ServiceResponse.<StoryJobStatusResponse>builder()
                        .success(true)
                        .message("Job status retrieved")
                        .data(result)
                        .timestamp(LocalDateTime.now())
                        .build()));
    }

    // =========================
    // Delete Endpoint
    // =========================
    @Operation(
            summary = "Delete an AdventureTube story owned by the caller",
            description = "Deletes the story identified by youtubeContentId. The caller's email is extracted " +
                    "from the Bearer token and forwarded to the geospatial service so deletion is scoped to " +
                    "the authenticated owner. Associated chapter screenshots are cleaned up downstream.",
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            in = ParameterIn.HEADER,
                            required = true,
                            description = "Bearer JWT issued by /auth/token.",
                            schema = @Schema(type = "string")
                    ),
                    @Parameter(
                            name = "youtubeContentId",
                            in = ParameterIn.PATH,
                            required = true,
                            description = "YouTube video ID of the story to delete.",
                            schema = @Schema(type = "string", example = "abc123XYZ")
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Story deleted. Body contains the tracking ID of the async screenshot-cleanup job.",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(type = "string", example = "trk_del_3f2c9b1e-7a4d-4f1c-9e2b-9b6a1d2c3e4f")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized — missing, malformed, or expired JWT.",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden — the story exists but is not owned by the caller.",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found — no story exists for the given youtubeContentId.",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service Unavailable — geospatial service unreachable.",
                    content = @Content(mediaType = "application/json")
            )
    })
    @DeleteMapping("/{youtubeContentId}")
    public Mono<ResponseEntity<ServiceResponse<StoryJobStatusResponse>>> deleteAdventureTubeData(
            @RequestHeader("Authorization") String authorization,
            @PathVariable String youtubeContentId) {
        log.info("DELETE /auth/geo/{} requested", youtubeContentId);
        return geoDataService.deleteByYoutubeContentId(authorization, youtubeContentId)
                .map(result -> ResponseEntity.ok().body(ServiceResponse.<StoryJobStatusResponse>builder()
                        .success(true)
                        .message("Story deleted successfully")
                        .data(result)
                        .timestamp(LocalDateTime.now())
                        .build()));
    }
}
