package com.adventuretube.geospatial.repository;

import com.adventuretube.geospatial.model.entity.ScreenshotJobStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ScreenshotJobStatusRepository extends MongoRepository<ScreenshotJobStatus, String> {
    Optional<ScreenshotJobStatus> findByYoutubeContentID(String youtubeContentID);
}
