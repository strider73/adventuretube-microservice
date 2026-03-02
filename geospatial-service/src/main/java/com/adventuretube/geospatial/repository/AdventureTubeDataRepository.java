package com.adventuretube.geospatial.repository;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdventureTubeDataRepository extends MongoRepository<AdventureTubeData, String> {
    Optional<AdventureTubeData> findByYoutubeContentID(String youtubeContentID);
    List<AdventureTubeData> findByUserContentType(String userContentType);
    List<AdventureTubeData> findByUserContentCategoryContaining(String category);
}
