package com.adventuretube.geospatial.service.jobstatus;


import com.adventuretube.geospatial.exceptions.JobNotFoundException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;
import com.adventuretube.geospatial.model.dto.ScreenshotJobStatusDTO;
import com.adventuretube.geospatial.model.entity.jobstatus.ScreenshotJobStatus;
import com.adventuretube.geospatial.model.enums.ScreenshotJobStatusEnum;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import com.adventuretube.geospatial.repository.ScreenshotJobStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenshotJobStatusService {
    private final ScreenshotJobStatusRepository screenshotJobStatusRepository;
    private final AdventureTubeDataRepository adventureTubeDataRepository;

    public ScreenshotJobStatus createPendingJob(String trackingId, String youtubeContentID) {
        //TODO: check the exsiting job status records because youtubeContentID is not unique if already exist
        screenshotJobStatusRepository.findByYoutubeContentID(youtubeContentID).ifPresent(screenshotJobStatusRepository::delete);

        return screenshotJobStatusRepository.save(ScreenshotJobStatus.builder()
                .trackingId(trackingId)
                .youtubeContentID(youtubeContentID)
                .status(ScreenshotJobStatusEnum.PENDING)
                .completedChapters(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .expireAt(LocalDateTime.now())
                .build());
    }

    public ScreenshotJobStatus markCompleted(String youtubeContentID, int completedChapters) {
        return updateStatus(youtubeContentID, ScreenshotJobStatusEnum.COMPLETED, null);
    }

    public ScreenshotJobStatus markFailed(String youtubeContentID, String errorMessage) {
        return updateStatus(youtubeContentID, ScreenshotJobStatusEnum.FAILED, errorMessage);
    }

    public Optional<ScreenshotJobStatus> findByYoutubeContentID(String youtubeContentID) {
        return screenshotJobStatusRepository.findByYoutubeContentID(youtubeContentID);
    }

    private ScreenshotJobStatus updateStatus(String youtubeContentID, ScreenshotJobStatusEnum status,
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

    public Optional<ScreenshotJobStatusDTO> getScreenshotStatusWithChapters(String youtubeContentID) {
        return screenshotJobStatusRepository.findByYoutubeContentID(youtubeContentID)
                .map(jobStatus -> {
                    List<ScreenshotJobStatusDTO.ChapterScreenshot> chapters =
                            (jobStatus.getStatus() == ScreenshotJobStatusEnum.COMPLETED)
                                    ? adventureTubeDataRepository.findByYoutubeContentID(youtubeContentID)//if job status is completed, get the chapters from adventure tube data
                                    .map(data -> data.getChapters().stream()//create the stream from the capter and iterate over each chapter
                                            .map(ch -> ScreenshotJobStatusDTO.ChapterScreenshot.builder()
                                                    .youtubeTime(ch.getYoutubeTime())
                                                    .screenshotUrl(ch.getScreenshotUrl())//to set the screenshot url
                                                    .build())
                                            .collect(Collectors.toList()))//collect the stream into a list
                                    .orElse(Collections.emptyList())
                                    : Collections.emptyList();//if job status is not completed, return empty list
                    //chapters has been set based on the job status
                    //create the ScreenshotJobStatusDTO  and set the chapters
                    return ScreenshotJobStatusDTO.builder()
                            .youtubeContentID(jobStatus.getYoutubeContentID())
                            .status(jobStatus.getStatus())
                            .totalChapters(jobStatus.getTotalChapters())
                            .completedChapters(jobStatus.getCompletedChapters())
                            .errorMessage(jobStatus.getErrorMessage())
                            .chapters(chapters)//set the chapters
                            .build();
                });
    }
}
