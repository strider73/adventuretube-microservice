package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.exceptions.JobNotFoundException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;
import com.adventuretube.geospatial.model.entity.PublishStoryJobStatus;
import com.adventuretube.geospatial.model.enums.PublishStoryJobStatusEnum;
import com.adventuretube.geospatial.repository.PublishStoryJobStatusRepository;
import com.adventuretube.geospatial.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishStoryJobStatusService {

    private final PublishStoryJobStatusRepository publishStoryJobStatusRepository;
    private final SseEmitterManager sseEmitterManager;

    public PublishStoryJobStatus createPendingJob(String trackingId, String youtubeContentID) {
        return publishStoryJobStatusRepository.save(PublishStoryJobStatus.builder()
                .trackingId(trackingId)
                .youtubeContentID(youtubeContentID)
                .status(PublishStoryJobStatusEnum.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expireAt(LocalDateTime.now())
                .build());
    }

    public PublishStoryJobStatus markCompleted(String trackingId, int chaptersCount, int placesCount) {
        return updateStatus(trackingId, PublishStoryJobStatusEnum.COMPLETED, null, chaptersCount, placesCount);
    }

    public PublishStoryJobStatus markCompletedWithDuplicate(String trackingId) {
        return updateStatus(trackingId, PublishStoryJobStatusEnum.DUPLICATED,"DUPLICATE YOUTUBE ID",0,0);
    }

    public PublishStoryJobStatus markFailed(String trackingId, String errorMessage) {
        return updateStatus(trackingId, PublishStoryJobStatusEnum.FAILED, errorMessage, 0, 0);
    }

    public Optional<PublishStoryJobStatus> findByTrackingId(String trackingId) {
        return publishStoryJobStatusRepository.findByTrackingId(trackingId);
    }

    private PublishStoryJobStatus updateStatus(String trackingId, PublishStoryJobStatusEnum status,
                                               String errorMessage, int chaptersCount, int placesCount) {
        PublishStoryJobStatus jobStatus = publishStoryJobStatusRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new JobNotFoundException(GeoErrorCode.JOB_NOT_FOUND));
        jobStatus.setStatus(status);
        jobStatus.setErrorMessage(errorMessage);
        jobStatus.setChaptersCount(chaptersCount);
        jobStatus.setPlacesCount(placesCount);
        jobStatus.setUpdatedAt(LocalDateTime.now());
        PublishStoryJobStatus saved = publishStoryJobStatusRepository.save(jobStatus);

        // Push update to SSE client (no-op if no client is listening)
        sseEmitterManager.send(trackingId, saved);
        return saved;
    }
}
