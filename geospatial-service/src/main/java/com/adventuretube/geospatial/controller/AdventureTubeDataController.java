package com.adventuretube.geospatial.controller;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.geospatial.exceptions.JobNotFoundException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;
import com.adventuretube.geospatial.kafka.Producer;
import com.adventuretube.geospatial.model.entity.JobStatus;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.model.enums.JobStatusEnum;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import com.adventuretube.geospatial.service.JobStatusService;
import com.adventuretube.geospatial.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/geo")
@RequiredArgsConstructor
public class AdventureTubeDataController {

    private final AdventureTubeDataService adventureTubeDataService;
    private final JobStatusService jobStatusService;
    private final Producer producer;
    private final SseEmitterManager sseEmitterManager;

    @GetMapping("/data")
    public ResponseEntity<List<AdventureTubeData>> findAll() {
        return ResponseEntity.ok(adventureTubeDataService.findAll());
    }

    @GetMapping("/data/{id}")
    public ResponseEntity<AdventureTubeData> findById(@PathVariable String id) {
        return adventureTubeDataService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/data/youtube/{youtubeContentID}")
    public ResponseEntity<AdventureTubeData> findByYoutubeContentID(@PathVariable String youtubeContentID) {
        return adventureTubeDataService.findByYoutubeContentID(youtubeContentID)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/data/type/{contentType}")
    public ResponseEntity<List<AdventureTubeData>> findByContentType(@PathVariable String contentType) {
        return ResponseEntity.ok(adventureTubeDataService.findByContentType(contentType));
    }

    @GetMapping("/data/category/{category}")
    public ResponseEntity<List<AdventureTubeData>> findByCategory(@PathVariable String category) {
        return ResponseEntity.ok(adventureTubeDataService.findByCategory(category));
    }

    @GetMapping("/data/count")
    public ResponseEntity<Long> count() {
        return ResponseEntity.ok(adventureTubeDataService.count());
    }

    @PutMapping("/data/{id}")
    public ResponseEntity<AdventureTubeData> update(@PathVariable String id, @RequestBody AdventureTubeData data) {
        log.info("PUT /geo/data/{} received", id);
        return ResponseEntity.ok(adventureTubeDataService.update(id, data));
    }

    @DeleteMapping("/data/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /geo/data/{} received", id);
        adventureTubeDataService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/data/delete/adventuretubedata")
    public ResponseEntity<Void> deleteByYoutubeContentID(@RequestParam String youtubeContentId, @RequestParam String ownerEmail) {
        log.info("DELETE /geo/data/delete/adventuretubedata youtubeContentId={}, ownerEmail={}", youtubeContentId, ownerEmail);
        adventureTubeDataService.deleteByYoutubeContentIdAndOwnerEmail(youtubeContentId, ownerEmail);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/save")
    public ResponseEntity<ServiceResponse<JobStatus>> save(@RequestBody AdventureTubeData data) {
        log.info("POST /geo/save received: youtubeContentID={}", data.getYoutubeContentID());

        String trackingId = UUID.randomUUID().toString();
        JobStatus pendingJob = jobStatusService.createPendingJob(trackingId, data.getYoutubeContentID());
        producer.sendAdventureTubeData(trackingId, data);

        ServiceResponse<JobStatus> response = ServiceResponse.<JobStatus>builder()
                .success(true)
                .message("Data accepted for processing")
                .data(pendingJob)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.accepted().body(response);
    }

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
