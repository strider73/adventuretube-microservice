package com.adventuretube.geospatial.controller;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping(value = "/geo")
@RequiredArgsConstructor
public class AdventureTubeDataController {

    private final AdventureTubeDataService adventureTubeDataService;

    @GetMapping("/data")
    public Mono<ResponseEntity<List<AdventureTubeData>>> findAll() {
        return Mono.fromCallable(adventureTubeDataService::findAll)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/data/{id}")
    public Mono<ResponseEntity<AdventureTubeData>> findById(@PathVariable String id) {
        return Mono.fromCallable(() -> adventureTubeDataService.findById(id))
                .map(opt -> opt.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()));
    }

    @GetMapping("/data/youtube/{youtubeContentID}")
    public Mono<ResponseEntity<AdventureTubeData>> findByYoutubeContentID(@PathVariable String youtubeContentID) {
        return Mono.fromCallable(() -> adventureTubeDataService.findByYoutubeContentID(youtubeContentID))
                .map(opt -> opt.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()));
    }

    @GetMapping("/data/type/{contentType}")
    public Mono<ResponseEntity<List<AdventureTubeData>>> findByContentType(@PathVariable String contentType) {
        return Mono.fromCallable(() -> adventureTubeDataService.findByContentType(contentType))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/data/category/{category}")
    public Mono<ResponseEntity<List<AdventureTubeData>>> findByCategory(@PathVariable String category) {
        return Mono.fromCallable(() -> adventureTubeDataService.findByCategory(category))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/data/count")
    public Mono<ResponseEntity<Long>> count() {
        return Mono.fromCallable(adventureTubeDataService::count)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/save")
    public Mono<ResponseEntity<AdventureTubeData>> save(@RequestBody AdventureTubeData data) {
        return Mono.fromCallable(() -> adventureTubeDataService.save(data))
                .map(ResponseEntity::ok);
    }
}
