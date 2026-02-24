package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AdventureTubeDataService {

    private final AdventureTubeDataRepository repository;

    public Flux<AdventureTubeData> findAll() {
        return repository.findAll();
    }

    public Mono<AdventureTubeData> findById(String id) {
        return repository.findById(id);
    }

    public Mono<AdventureTubeData> findByYoutubeContentID(String youtubeContentID) {
        return repository.findByYoutubeContentID(youtubeContentID);
    }

    public Flux<AdventureTubeData> findByContentType(String contentType) {
        return repository.findByUserContentType(contentType);
    }

    public Flux<AdventureTubeData> findByCategory(String category) {
        return repository.findByUserContentCategoryContaining(category);
    }

    public Mono<AdventureTubeData> save(AdventureTubeData data) {
        return repository.save(data);
    }

    public Mono<Long> count() {
        return repository.count();
    }
}
