package com.adventuretube.geospatial.controller;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.geospatial.kafka.Producer;
import com.adventuretube.geospatial.model.entity.JobStatus;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import com.adventuretube.geospatial.service.JobStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

//    @DeleteMapping("/data/{id}")
//    public ResponseEntity<Void> delete(@PathVariable String id) {
//        log.info("DELETE /geo/data/{} received", id);
//        adventureTubeDataService.delete(id);
//        return ResponseEntity.noContent().build();
//    }

    @DeleteMapping("/data/delete/adventuretubedata")
    public ResponseEntity<ServiceResponse<JobStatus>> deleteByYoutubeContentID(@RequestParam String youtubeContentId, @RequestParam String ownerEmail) {
        log.info("DELETE /geo/data/delete/adventuretubedata youtubeContentId={}, ownerEmail={}", youtubeContentId, ownerEmail);
        String trackingId = UUID.randomUUID().toString();
        JobStatus pendingJob = jobStatusService.createPendingJob(trackingId,youtubeContentId);
        producer.deleteAdventureTubeData(trackingId, youtubeContentId, ownerEmail);
        ServiceResponse<JobStatus> response = ServiceResponse.<JobStatus>builder()
                .success(true)
                .message("Deletion accepted and processing")
                .data(pendingJob)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/save")
    public ResponseEntity<ServiceResponse<JobStatus>> save(@RequestBody AdventureTubeData data) {
        log.info("POST /geo/save received: youtubeContentID={}", data.getYoutubeContentID());

        String trackingId = UUID.randomUUID().toString();
        JobStatus pendingJob = jobStatusService.createPendingJob(trackingId, data.getYoutubeContentID());
        producer.sendAdventureTubeData(trackingId, data);

        ServiceResponse<JobStatus> response = ServiceResponse.<JobStatus>builder()
                .success(true)
                .message("Data accepted and processing")
                .data(pendingJob)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.accepted().body(response);
    }

}
