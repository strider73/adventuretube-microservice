package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.exceptions.OwnershipMismatchException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdventureTubeDataService {

    private final AdventureTubeDataRepository repository;
    private final MongoTemplate mongoTemplate;

    // Called by: AdventureTubeDataController (REST)
    public List<AdventureTubeData> findAll() {
        return repository.findAll();
    }

    // Called by: AdventureTubeDataController (REST)
    public Optional<AdventureTubeData> findById(String id) {
        return repository.findById(id);
    }

    // Called by: AdventureTubeDataController (REST) + StoryConsumer.handleDelete() (Kafka)
    public Optional<AdventureTubeData> findByYoutubeContentID(String youtubeContentID) {
        return repository.findByYoutubeContentID(youtubeContentID);
    }

    // Called by: AdventureTubeDataController (REST)
    public List<AdventureTubeData> findByContentType(String contentType) {
        return repository.findByUserContentType(contentType);
    }

    // Called by: AdventureTubeDataController (REST)
    public List<AdventureTubeData> findByCategory(String category) {
        return repository.findByUserContentCategoryContaining(category);
    }

    // Called by: StoryConsumer.handleSave() (Kafka consumer only)
    public AdventureTubeData save(AdventureTubeData data) {
        return repository.save(data);
    }

    // Called by: AdventureTubeDataController (REST)
    public AdventureTubeData update(String id, AdventureTubeData data) {
        return repository.findById(id)
                .map(existing -> {
                    data.setId(id);
                    return repository.save(data);
                })
                .orElseThrow(() -> new IllegalArgumentException("AdventureTubeData not found with id: " + id));
    }

//    public void delete(String id) {
//        if (!repository.existsById(id)) {
//            throw new IllegalArgumentException("AdventureTubeData not found with id: " + id);
//        }
//        repository.deleteById(id);
//    }

    // Called by: StoryConsumer.handleDelete() (Kafka consumer only)
    public void deleteByYoutubeContentIdAndOwnerEmail(String youtubeContentId, String ownerEmail) {
        AdventureTubeData data = repository.findByYoutubeContentID(youtubeContentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "AdventureTubeData not found with youtubeContentID: " + youtubeContentId));

        if (!data.getOwnerEmail().equals(ownerEmail)) {
            throw new OwnershipMismatchException(GeoErrorCode.OWNERSHIP_MISMATCH);
        }

        repository.deleteById(data.getId());
    }


    // Called by: AdventureTubeDataController (REST)
    public List<AdventureTubeData> findWithinBounds(double swLng, double swLat, double neLng, double neLat) {
        Query query = new Query(
                Criteria.where("places.location")
                        .within(new Box(new Point(swLng, swLat), new Point(neLng, neLat)))
        );
        return mongoTemplate.find(query, AdventureTubeData.class);
    }

    // Called by: AdventureTubeDataController (REST)
    public long count() {
        return repository.count();
    }
}
