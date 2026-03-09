package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.exceptions.JobNotFoundException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;
import com.adventuretube.geospatial.model.entity.JobStatus;
import com.adventuretube.geospatial.model.enums.JobStatusEnum;
import com.adventuretube.geospatial.repository.JobStatusRepository;
import com.adventuretube.geospatial.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobStatusService {

    private final JobStatusRepository jobStatusRepository;
    private final SseEmitterManager sseEmitterManager;

    public JobStatus createPendingJob(String trackingId, String youtubeContentID) {
        JobStatus jobStatus = new JobStatus();
        jobStatus.setTrackingId(trackingId);
        jobStatus.setYoutubeContentID(youtubeContentID);
        jobStatus.setStatus(JobStatusEnum.PENDING);
        jobStatus.setCreatedAt(LocalDateTime.now());
        jobStatus.setUpdatedAt(LocalDateTime.now());
        jobStatus.setExpireAt(LocalDateTime.now());
        return jobStatusRepository.save(jobStatus);
    }

    public JobStatus markCompleted(String trackingId, int chaptersCount, int placesCount) {
        return updateStatus(trackingId, JobStatusEnum.COMPLETED, null, chaptersCount, placesCount);
    }

    public JobStatus markDuplicate(String trackingId) {
        return updateStatus(trackingId, JobStatusEnum.DUPLICATE, "Duplicate youtubeContentID", 0, 0);
    }

    public JobStatus markFailed(String trackingId, String errorMessage) {
        return updateStatus(trackingId, JobStatusEnum.FAILED, errorMessage, 0, 0);
    }

    public Optional<JobStatus> findByTrackingId(String trackingId) {
        return jobStatusRepository.findByTrackingId(trackingId);
    }

    private JobStatus updateStatus(String trackingId, JobStatusEnum status,
                                   String errorMessage, int chaptersCount, int placesCount) {
        JobStatus jobStatus = jobStatusRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new JobNotFoundException(GeoErrorCode.JOB_NOT_FOUND));
        jobStatus.setStatus(status);
        jobStatus.setErrorMessage(errorMessage);
        jobStatus.setChaptersCount(chaptersCount);
        jobStatus.setPlacesCount(placesCount);
        jobStatus.setUpdatedAt(LocalDateTime.now());
        JobStatus saved = jobStatusRepository.save(jobStatus);

        // Push update to SSE client (no-op if no client is listening)
        sseEmitterManager.send(trackingId, saved);
        return saved;
    }
}
