package com.adventuretube.geospatial.controller;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.geospatial.exceptions.JobNotFoundException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;
import com.adventuretube.geospatial.model.entity.jobstatus.StoryJobStatus;
import com.adventuretube.geospatial.model.enums.StoryJobStatusEnum;
import com.adventuretube.geospatial.service.jobstatus.StoryJobStatusService;
import com.adventuretube.geospatial.sse.SseEmitterManager;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping(value = "/geo")
@RequiredArgsConstructor
@Tag(name = "Job Status Controller", description = "Job status polling and SSE streaming for async geospatial operations.")
public class StoryJobStatusSSEController {

    private final StoryJobStatusService storyJobStatusService;
    private final SseEmitterManager sseEmitterManager;

    @Operation(summary = "Stream job status updates via Server-Sent Events")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SSE stream opened."),
            @ApiResponse(responseCode = "404", description = "Job not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Job status not found",
                                      "errorCode": "JOB_NOT_FOUND",
                                      "data": null,
                                      "timestamp": "2026-03-23T14:00:00"
                                    }
                                    """)))
    })
    @GetMapping(value = "/status/stream/{trackingId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamJobStatus(@PathVariable String trackingId) {
        log.info("SSE /geo/status/stream/{} requested", trackingId);

        StoryJobStatus jobStatus = storyJobStatusService.findByTrackingId(trackingId)
                .orElseThrow(() -> new JobNotFoundException(GeoErrorCode.JOB_NOT_FOUND));

        // If already terminal, return immediately and complete
        if (jobStatus.getStatus() != StoryJobStatusEnum.PENDING) {
            log.info("SSE /geo/status/stream/{} already terminal ({}), sending immediately", trackingId, jobStatus.getStatus());
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event()
                        .name("job-status")
                        .data(jobStatus, MediaType.APPLICATION_JSON));
                emitter.complete();
                log.info("SSE /geo/status/stream/{} completed", trackingId);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // Register for future updates
        SseEmitter emitter = sseEmitterManager.register(trackingId, 30_000L);

        // Send initial PENDING status
        try {
            emitter.send(SseEmitter.event()
                    .name("job-status")
                    .data(jobStatus, MediaType.APPLICATION_JSON));
            log.info("SSE /geo/status/stream/{} sent PENDING, waiting for Kafka consumer", trackingId);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @Operation(summary = "Get job status by tracking ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Job status retrieved."),
            @ApiResponse(responseCode = "404", description = "Job not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Job status not found",
                                      "errorCode": "JOB_NOT_FOUND",
                                      "data": null,
                                      "timestamp": "2026-03-23T14:00:00"
                                    }
                                    """)))
    })
    @GetMapping("/status/{trackingId}")
    public ResponseEntity<ServiceResponse<StoryJobStatus>> getJobStatus(@PathVariable String trackingId) {
        log.info("GET /geo/status/{} requested", trackingId);
        StoryJobStatus jobStatus = storyJobStatusService.findByTrackingId(trackingId)
                .orElseThrow(() -> new JobNotFoundException(GeoErrorCode.JOB_NOT_FOUND));

        ServiceResponse<StoryJobStatus> response = ServiceResponse.<StoryJobStatus>builder()
                .success(true)
                .message("Job status retrieved")
                .data(jobStatus)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }
}
