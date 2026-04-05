package com.adventuretube.geospatial.repository;

import com.adventuretube.geospatial.model.entity.jobstatus.StoryJobStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoryJobStatusRepository extends MongoRepository<StoryJobStatus, String> {
    Optional<StoryJobStatus> findByTrackingId(String trackingId);
    void deleteByYoutubeContentID(String youtubeContentID);
    void deleteByTrackingId(String trackingId);
}
