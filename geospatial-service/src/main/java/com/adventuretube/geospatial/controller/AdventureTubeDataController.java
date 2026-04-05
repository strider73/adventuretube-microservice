package com.adventuretube.geospatial.controller;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.geospatial.kafka.story.StoryProducer;
import com.adventuretube.geospatial.model.entity.jobstatus.StoryJobStatus;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import com.adventuretube.geospatial.service.jobstatus.StoryJobStatusService;
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
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(value = "/geo")
@RequiredArgsConstructor
@Tag(name = "Geospatial Data Controller", description = "CRUD operations for AdventureTube geospatial data. Internal service called by auth-service and web-service.")
public class AdventureTubeDataController {

    private final AdventureTubeDataService adventureTubeDataService;
    private final StoryJobStatusService storyJobStatusService;
    private final StoryProducer storyProducer;

    @Operation(summary = "Get all geospatial data")
    @ApiResponse(responseCode = "200", description = "All geospatial data retrieved.")
    @GetMapping("/data")
    public ResponseEntity<List<AdventureTubeData>> findAll() {
        return ResponseEntity.ok(adventureTubeDataService.findAll());
    }

    @Operation(summary = "Get geospatial data by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data found."),
            @ApiResponse(responseCode = "404", description = "Data not found.")
    })
    @GetMapping("/data/{id}")
    public ResponseEntity<AdventureTubeData> findById(@PathVariable String id) {
        return adventureTubeDataService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get geospatial data by YouTube content ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Data found."),
            @ApiResponse(responseCode = "404", description = "Data not found.")
    })
    @GetMapping("/data/youtube/{youtubeContentID}")
    public ResponseEntity<AdventureTubeData> findByYoutubeContentID(@PathVariable String youtubeContentID) {
        return adventureTubeDataService.findByYoutubeContentID(youtubeContentID)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get geospatial data by content type")
    @ApiResponse(responseCode = "200", description = "Data retrieved.")
    @GetMapping("/data/type/{contentType}")
    public ResponseEntity<List<AdventureTubeData>> findByContentType(@PathVariable String contentType) {
        return ResponseEntity.ok(adventureTubeDataService.findByContentType(contentType));
    }

    @Operation(summary = "Get geospatial data by category")
    @ApiResponse(responseCode = "200", description = "Data retrieved.")
    @GetMapping("/data/category/{category}")
    public ResponseEntity<List<AdventureTubeData>> findByCategory(@PathVariable String category) {
        return ResponseEntity.ok(adventureTubeDataService.findByCategory(category));
    }

    @Operation(summary = "Get total count of geospatial data")
    @ApiResponse(responseCode = "200", description = "Count retrieved.")
    @GetMapping("/data/count")
    public ResponseEntity<Long> count() {
        return ResponseEntity.ok(adventureTubeDataService.count());
    }

    @Operation(summary = "Get geospatial data within bounding box")
    @ApiResponse(responseCode = "200", description = "Data within bounds retrieved.")
    @GetMapping("/data/bounds")
    public ResponseEntity<List<AdventureTubeData>> findWithinBounds(
            @RequestParam double swLat,
            @RequestParam double swLng,
            @RequestParam double neLat,
            @RequestParam double neLng) {
        log.info("GET /geo/data/bounds swLat={}, swLng={}, neLat={}, neLng={}", swLat, swLng, neLat, neLng);
        return ResponseEntity.ok(adventureTubeDataService.findWithinBounds(swLng, swLat, neLng, neLat));
    }

    @Operation(summary = "Update geospatial data by ID")
    @ApiResponse(responseCode = "200", description = "Data updated.")
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

    @Operation(summary = "Delete geospatial data by YouTube content ID (async via Kafka)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deletion accepted and processing."),
            @ApiResponse(responseCode = "403", description = "Ownership mismatch.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "AdventuretubeData ownership email is not matched",
                                      "errorCode": "OWNERSHIP_MISMATCH",
                                      "data": null,
                                      "timestamp": "2026-03-23T14:00:00"
                                    }
                                    """)))
    })
    @DeleteMapping("/data/delete/adventuretubedata")
    public ResponseEntity<ServiceResponse<StoryJobStatus>> deleteByYoutubeContentID(@RequestParam String youtubeContentId, @RequestParam String ownerEmail) {
        log.info("DELETE /geo/data/delete/adventuretubedata youtubeContentId={}, ownerEmail={}", youtubeContentId, ownerEmail);
        String trackingId = UUID.randomUUID().toString();
        StoryJobStatus pendingJob = storyJobStatusService.createPendingJob(trackingId,youtubeContentId);
        storyProducer.deleteAdventureTubeData(trackingId, youtubeContentId, ownerEmail);
        ServiceResponse<StoryJobStatus> response = ServiceResponse.<StoryJobStatus>builder()
                .success(true)
                .message("Deletion accepted and processing")
                .data(pendingJob)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Save geospatial data (async via Kafka)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Data accepted and processing."),
            @ApiResponse(responseCode = "409", description = "Duplicate data.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Duplicate entry already exists",
                                      "errorCode": "DUPLICATE_KEY",
                                      "data": null,
                                      "timestamp": "2026-03-23T14:00:00"
                                    }
                                    """)))
    })
    @PostMapping("/save")
    public ResponseEntity<ServiceResponse<StoryJobStatus>> save(@RequestBody AdventureTubeData data) {
        log.info("POST /geo/save received: youtubeContentID={}", data.getYoutubeContentID());

        String trackingId = UUID.randomUUID().toString();
        StoryJobStatus pendingJob = storyJobStatusService.createPendingJob(trackingId, data.getYoutubeContentID());
        storyProducer.sendAdventureTubeData(trackingId, data);

        ServiceResponse<StoryJobStatus> response = ServiceResponse.<StoryJobStatus>builder()
                .success(true)
                .message("Data accepted and processing")
                .data(pendingJob)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.accepted().body(response);
    }

}
