package com.adventuretube.web.controller;


import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.web.service.ChapterScreenShotService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/web/chapter")
@RequiredArgsConstructor
@Tag(name = "Web Chapter Screenshot Controller",
     description = "Public-facing chapter screenshot endpoint. Pass-through proxy to geospatial-service /geo/screenshot via reactive ServiceClient.")
public class ChapterScreenShotController {

    private final ChapterScreenShotService chapterScreenShotService;

    @Operation(
            summary = "Get chapter screenshot status and URLs (proxy)",
            description = """
                    Public-facing endpoint that proxies to geospatial-service. Returns the upstream
                    `ServiceResponse<ChapterScreenshotDTO>` payload (status + chapters[].screenshotUrl)
                    wrapped as `ServiceResponse<JsonNode>`. iOS prepends
                    `https://s3.travel-tube.com/chapter-screenshots/` to each `screenshotUrl` to build the full URL.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Screenshot status retrieved from geospatial-service.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = {
                                    @ExampleObject(name = "COMPLETED",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "Screenshot status retrieved",
                                                      "data": {
                                                        "success": true,
                                                        "message": "Screenshot status retrieved",
                                                        "data": {
                                                          "youtubeContentID": "xlumX1Wtzrg",
                                                          "status": "COMPLETED",
                                                          "totalChapters": 4,
                                                          "completedChapters": 4,
                                                          "chapters": [
                                                            { "youtubeTime": 4,    "screenshotUrl": "xlumX1Wtzrg/ch1_4s.jpg" },
                                                            { "youtubeTime": 397,  "screenshotUrl": "xlumX1Wtzrg/ch2_397s.jpg" }
                                                          ]
                                                        },
                                                        "timestamp": "2026-04-29T10:00:00"
                                                      },
                                                      "timestamp": "2026-04-29T10:00:00"
                                                    }
                                                    """),
                                    @ExampleObject(name = "PENDING",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "Screenshot status retrieved",
                                                      "data": {
                                                        "success": true,
                                                        "data": {
                                                          "status": "PENDING",
                                                          "chapters": []
                                                        }
                                                      },
                                                      "timestamp": "2026-04-29T10:00:00"
                                                    }
                                                    """),
                                    @ExampleObject(name = "NO_JOB_FOUND",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "Screenshot status retrieved",
                                                      "data": {
                                                        "success": true,
                                                        "message": "No screenshot job found"
                                                      },
                                                      "timestamp": "2026-04-29T10:00:00"
                                                    }
                                                    """)
                            })),
            @ApiResponse(responseCode = "503", description = "Geospatial service unavailable.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Service call failed",
                                      "errorCode": "SERVICE_UNAVAILABLE",
                                      "data": "GEOSPATIAL-SERVICE : web-service",
                                      "timestamp": "2026-04-29T10:00:00"
                                    }
                                    """)))
    })
    @GetMapping("/screenshot/{youtubeContentId}")
    public Mono<ResponseEntity<ServiceResponse<JsonNode>>> getScreenshotStatus(
            @Parameter(description = "YouTube content ID (e.g. `xlumX1Wtzrg`)", example = "xlumX1Wtzrg")
            @PathVariable String youtubeContentId) {
        return chapterScreenShotService.getScreenshotStatus(youtubeContentId)
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
