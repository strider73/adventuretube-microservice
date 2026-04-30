package com.adventuretube.geospatial.service.jobstatus;


import com.adventuretube.geospatial.exceptions.JobNotFoundException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;
import com.adventuretube.geospatial.model.entity.jobstatus.ScreenshotJobStatus;
import com.adventuretube.geospatial.model.enums.ChapterScreenshotJobStatusEnum;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import com.adventuretube.geospatial.repository.ScreenshotJobStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterScreenshotJobStatusService {
    private final ScreenshotJobStatusRepository screenshotJobStatusRepository;
    private final AdventureTubeDataRepository adventureTubeDataRepository;

    public ScreenshotJobStatus createPendingJob(String trackingId, String youtubeContentID) {
        //TODO: check the exsiting job status records because youtubeContentID is not unique if already exist
        screenshotJobStatusRepository.findByYoutubeContentID(youtubeContentID).ifPresent(screenshotJobStatusRepository::delete);

        return screenshotJobStatusRepository.save(ScreenshotJobStatus.builder()
                .trackingId(trackingId)
                .youtubeContentID(youtubeContentID)
                .status(ChapterScreenshotJobStatusEnum.PENDING)
                .completedChapters(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expireAt(LocalDateTime.now())
                .build());
    }

    public ScreenshotJobStatus markCompleted(String youtubeContentID, int completedChapters) {
        return updateStatus(youtubeContentID, ChapterScreenshotJobStatusEnum.COMPLETED, null);
    }

    public ScreenshotJobStatus markFailed(String youtubeContentID, String errorMessage) {
        return updateStatus(youtubeContentID, ChapterScreenshotJobStatusEnum.FAILED, errorMessage);
    }

    public Optional<ScreenshotJobStatus> findByYoutubeContentID(String youtubeContentID) {
        return screenshotJobStatusRepository.findByYoutubeContentID(youtubeContentID);
    }

    private ScreenshotJobStatus updateStatus(String youtubeContentID, ChapterScreenshotJobStatusEnum status,
                                             String errorMessage) {
        ScreenshotJobStatus jobStatus = screenshotJobStatusRepository.findByYoutubeContentID(youtubeContentID)
                .orElseThrow(() -> new JobNotFoundException(GeoErrorCode.JOB_NOT_FOUND));
        jobStatus.setStatus(status);
        jobStatus.setErrorMessage(errorMessage);
        jobStatus.setUpdatedAt(LocalDateTime.now());
        return screenshotJobStatusRepository.save(jobStatus);
    }

    public void deleteByYoutubeContentID(String youtubeContentId) {
        screenshotJobStatusRepository.deleteByYoutubeContentID(youtubeContentId);
    }


    public Optional<ScreenshotJobStatus> getScreenshotJobStatus(String youtubeContentID) {
        return screenshotJobStatusRepository.findByYoutubeContentID(youtubeContentID);
    }


}
