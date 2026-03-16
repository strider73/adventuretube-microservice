package com.adventuretube.geospatial.controller;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.geospatial.exceptions.JobNotFoundException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;
import com.adventuretube.geospatial.model.entity.JobStatus;
import com.adventuretube.geospatial.model.enums.JobStatusEnum;
import com.adventuretube.geospatial.service.JobStatusService;
import com.adventuretube.geospatial.sse.SseEmitterManager;
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
public class JobStatusSSEController {

    private final JobStatusService jobStatusService;
    private final SseEmitterManager sseEmitterManager;

    @GetMapping(value = "/status/stream/{trackingId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamJobStatus(@PathVariable String trackingId) {
        log.info("SSE /geo/status/stream/{} requested", trackingId);

        JobStatus jobStatus = jobStatusService.findByTrackingId(trackingId)
                .orElseThrow(() -> new JobNotFoundException(GeoErrorCode.JOB_NOT_FOUND));

        // If already terminal, return immediately and complete
        if (jobStatus.getStatus() != JobStatusEnum.PENDING) {
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

    @GetMapping("/status/{trackingId}")
    public ResponseEntity<ServiceResponse<JobStatus>> getJobStatus(@PathVariable String trackingId) {
        log.info("GET /geo/status/{} requested", trackingId);
        JobStatus jobStatus = jobStatusService.findByTrackingId(trackingId)
                .orElseThrow(() -> new JobNotFoundException(GeoErrorCode.JOB_NOT_FOUND));

        ServiceResponse<JobStatus> response = ServiceResponse.<JobStatus>builder()
                .success(true)
                .message("Job status retrieved")
                .data(jobStatus)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }
}
