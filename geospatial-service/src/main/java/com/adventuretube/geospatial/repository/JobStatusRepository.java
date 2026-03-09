package com.adventuretube.geospatial.repository;

import com.adventuretube.geospatial.model.entity.JobStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobStatusRepository extends MongoRepository<JobStatus, String> {
    Optional<JobStatus> findByTrackingId(String trackingId);
}
