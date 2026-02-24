package com.adventuretube.geospatial.repository;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AdventureTubeDataRepository extends ReactiveMongoRepository<AdventureTubeData, String> {
    Mono<AdventureTubeData> findByYoutubeContentID(String youtubeContentID);
    Flux<AdventureTubeData> findByUserContentType(String userContentType);
    Flux<AdventureTubeData> findByUserContentCategoryContaining(String category);
}
