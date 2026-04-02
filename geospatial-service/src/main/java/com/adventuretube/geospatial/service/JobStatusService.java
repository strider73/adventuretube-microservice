package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.exceptions.JobNotFoundException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;
import com.adventuretube.geospatial.model.entity.StoryJobStatus;
import com.adventuretube.geospatial.model.enums.StoryJobStatusEnum;
import com.adventuretube.geospatial.repository.StoryJobStatusRepository;
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

    private final StoryJobStatusRepository storyJobStatusRepository;
    private final SseEmitterManager sseEmitterManager;

    public StoryJobStatus createPendingJob(String trackingId, String youtubeContentID) {
        return storyJobStatusRepository.save(StoryJobStatus.builder()
                .trackingId(trackingId)
                .youtubeContentID(youtubeContentID)
                .status(StoryJobStatusEnum.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expireAt(LocalDateTime.now())
                .build());
    }

    public StoryJobStatus markCompleted(String trackingId, int chaptersCount, int placesCount) {
        return updateStatus(trackingId, StoryJobStatusEnum.COMPLETED, null, chaptersCount, placesCount);
    }

    public StoryJobStatus markCompletedWithDuplicate(String trackingId) {
        return updateStatus(trackingId, StoryJobStatusEnum.DUPLICATED,"DUPLICATE YOUTUBE ID",0,0);
    }

    public StoryJobStatus markFailed(String trackingId, String errorMessage) {
        return updateStatus(trackingId, StoryJobStatusEnum.FAILED, errorMessage, 0, 0);
    }

    public Optional<StoryJobStatus> findByTrackingId(String trackingId) {
        return storyJobStatusRepository.findByTrackingId(trackingId);
    }

    private StoryJobStatus updateStatus(String trackingId, StoryJobStatusEnum status,
                                        String errorMessage, int chaptersCount, int placesCount) {
        StoryJobStatus jobStatus = storyJobStatusRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new JobNotFoundException(GeoErrorCode.JOB_NOT_FOUND));
        jobStatus.setStatus(status);
        jobStatus.setErrorMessage(errorMessage);
        jobStatus.setChaptersCount(chaptersCount);
        jobStatus.setPlacesCount(placesCount);
        jobStatus.setUpdatedAt(LocalDateTime.now());
        StoryJobStatus saved = storyJobStatusRepository.save(jobStatus);

        // Push update to SSE client (no-op if no client is listening)
        sseEmitterManager.send(trackingId, saved);
        return saved;
    }
}
