package com.adventuretube.geospatial.controller;

import com.adventuretube.geospatial.kafka.Producer;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/geo")
@RequiredArgsConstructor
public class AdventureTubeDataController {

    private final AdventureTubeDataService adventureTubeDataService;
    private final Producer producer;

    @GetMapping("/data")
    public Mono<ResponseEntity<List<AdventureTubeData>>> findAll() {
        return adventureTubeDataService.findAll()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/data/{id}")
    public Mono<ResponseEntity<AdventureTubeData>> findById(@PathVariable String id) {
        return adventureTubeDataService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/data/youtube/{youtubeContentID}")
    public Mono<ResponseEntity<AdventureTubeData>> findByYoutubeContentID(@PathVariable String youtubeContentID) {
        return adventureTubeDataService.findByYoutubeContentID(youtubeContentID)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/data/type/{contentType}")
    public Mono<ResponseEntity<List<AdventureTubeData>>> findByContentType(@PathVariable String contentType) {
        return adventureTubeDataService.findByContentType(contentType)
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/data/category/{category}")
    public Mono<ResponseEntity<List<AdventureTubeData>>> findByCategory(@PathVariable String category) {
        return adventureTubeDataService.findByCategory(category)
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/data/count")
    public Mono<ResponseEntity<Long>> count() {
        return adventureTubeDataService.count()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/save")
    public Mono<ResponseEntity<String>> save(@RequestBody AdventureTubeData data) {
        log.info("POST /geo/save received: youtubeContentID={}", data.getYoutubeContentID());
        producer.sendAdventureTubeData(data);
        return Mono.just(ResponseEntity.accepted()
                .body("Data accepted: youtubeContentID=" + data.getYoutubeContentID()));
    }
}
