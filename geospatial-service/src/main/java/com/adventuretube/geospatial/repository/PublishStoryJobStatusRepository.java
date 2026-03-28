package com.adventuretube.geospatial.repository;

import com.adventuretube.geospatial.model.entity.PublishStoryJobStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublishStoryJobStatusRepository extends MongoRepository<PublishStoryJobStatus, String> {
    Optional<PublishStoryJobStatus> findByTrackingId(String trackingId);
}
