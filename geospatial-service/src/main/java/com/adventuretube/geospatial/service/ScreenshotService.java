package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.kafka.screenshot.ScreenshotProducer;
import com.adventuretube.geospatial.model.dto.ChapterScreenshotDTO;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.model.enums.ScreenshotJobStatusEnum;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import com.adventuretube.geospatial.service.jobstatus.ScreenshotJobStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenshotService {
    private final AdventureTubeDataRepository adventureTubeDataRepository;
    private final ScreenshotJobStatusService screenshotJobStatusService;
    private final ScreenshotProducer screenshotProducer;


    public void deleteScreenshots(String youtubeContentID, String trackingId,AdventureTubeData adventureTubeData) {

        screenshotProducer.deleteScreenshotRequest(youtubeContentID, trackingId, adventureTubeData);
        log.info("All screenshots deleted reqeust has been sent for  {}", youtubeContentID);


    }

    /**
     * Get screenshot job status with chapter screenshots
     *
     * @param youtubeContentID
     * @return
     */
    public Optional<ChapterScreenshotDTO> getScreenshotWithStatus(String youtubeContentID) {
        return screenshotJobStatusService.getScreenshotJobStatus(youtubeContentID)
                .map(jobStatus -> {
                    List<ChapterScreenshotDTO.ChapterScreenshot> chapters =
                            (jobStatus.getStatus() == ScreenshotJobStatusEnum.COMPLETED)
                                    ? adventureTubeDataRepository.findByYoutubeContentID(youtubeContentID)//if job status is completed, get the chapters from adventure tube data
                                      .map(data -> data.getChapters().stream()//create the stream from the capter and iterate over each chapter
                                                   .map(ch -> ChapterScreenshotDTO.ChapterScreenshot.builder()
                                                              .youtubeTime(ch.getYoutubeTime())
                                                              .screenshotUrl(ch.getScreenshotUrl())//to set the screenshot url
                                                              .build())
                                                   .collect(Collectors.toList()))//collect the stream into a list
                                      .orElse(Collections.emptyList())
                                    : Collections.emptyList();//if job status is not completed, return empty list
                    //chapters has been set based on the job status
                    //create the ScreenshotJobStatusDTO  and set the chapters
                    return ChapterScreenshotDTO.builder()
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
