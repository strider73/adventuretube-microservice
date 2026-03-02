package com.adventuretube.geospatial.controller;

import com.adventuretube.geospatial.kafka.Producer;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/geo")
@RequiredArgsConstructor
public class AdventureTubeDataController {

    private final AdventureTubeDataService adventureTubeDataService;
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

    @DeleteMapping("/data/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /geo/data/{} received", id);
        adventureTubeDataService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/save")
    public ResponseEntity<String> save(@RequestBody AdventureTubeData data) {
        log.info("POST /geo/save received: youtubeContentID={}", data.getYoutubeContentID());
        producer.sendAdventureTubeData(data);
        return ResponseEntity.accepted()
                .body("Data accepted: youtubeContentID=" + data.getYoutubeContentID());
    }
}
