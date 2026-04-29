package com.adventuretube.geospatial.controller;


import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.geospatial.model.dto.ChapterScreenshotDTO;
import com.adventuretube.geospatial.service.ScreenshotService;
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

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/geo/screenshot")
@RequiredArgsConstructor
@Tag(name = "Geo Screenshot Controller",
     description = "Internal endpoint for chapter screenshot job status and URLs. Reads ScreenshotJobStatus + AdventureTubeData.chapters[].screenshotUrl from MongoDB.")
public class ScreenShotController {

    private final ScreenshotService screenshotService;


    @Operation(
            summary = "Get chapter screenshot status and URLs by YouTube content ID",
            description = """
                    Returns the screenshot job status (PENDING/COMPLETED/FAILED) along with chapter screenshot URLs.
                    Chapter URLs are populated only when status is COMPLETED. URLs are stored as partial paths
                    (e.g. `xlumX1Wtzrg/ch1_4s.jpg`) — full S3 URL is constructed by prepending
                    `https://s3.travel-tube.com/chapter-screenshots/` on the client side.
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Screenshot status retrieved (job exists) OR no job found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = {
                                    @ExampleObject(name = "COMPLETED",
                                            value = """
                                                    {
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
                                                    }
                                                    """),
                                    @ExampleObject(name = "PENDING",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "Screenshot status retrieved",
                                                      "data": {
                                                        "youtubeContentID": "xlumX1Wtzrg",
                                                        "status": "PENDING",
                                                        "totalChapters": 4,
                                                        "completedChapters": 0,
                                                        "chapters": []
                                                      },
                                                      "timestamp": "2026-04-29T10:00:00"
                                                    }
                                                    """),
                                    @ExampleObject(name = "FAILED",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "Screenshot status retrieved",
                                                      "data": {
                                                        "youtubeContentID": "xlumX1Wtzrg",
                                                        "status": "FAILED",
                                                        "totalChapters": 4,
                                                        "completedChapters": 0,
                                                        "errorMessage": "yt-dlp failed to download video",
                                                        "chapters": []
                                                      },
                                                      "timestamp": "2026-04-29T10:00:00"
                                                    }
                                                    """),
                                    @ExampleObject(name = "NO_JOB_FOUND",
                                            value = """
                                                    {
                                                      "success": true,
                                                      "message": "No screenshot job found",
                                                      "timestamp": "2026-04-29T10:00:00"
                                                    }
                                                    """)
                            }))
    })
    @GetMapping("/{youtubeContentId}")
    public ResponseEntity<ServiceResponse<ChapterScreenshotDTO>> getScreenshotStatus(
            @Parameter(description = "YouTube content ID (e.g. `xlumX1Wtzrg`)", example = "xlumX1Wtzrg")
            @PathVariable String youtubeContentId) {
        //2. wrap the optional value in a ServiceResponse
        ServiceResponse<ChapterScreenshotDTO>   response  = screenshotService.getScreenshotWithStatus(youtubeContentId)
                //dto will get return with optional so map with orElseGet will be able to handle both cases
                .map(dto -> ServiceResponse.<ChapterScreenshotDTO>builder()
                        .success(true)
                        .message("Screenshot status retrieved")
                        .data(dto)
                        .timestamp(LocalDateTime.now())
                        .build())
                .orElseGet(() -> ServiceResponse.<ChapterScreenshotDTO>builder()
                        .success(true)
                        .message("No screenshot job found")
                        .timestamp(LocalDateTime.now())
                        .build());

        return ResponseEntity.ok().body(response);


    }
}
