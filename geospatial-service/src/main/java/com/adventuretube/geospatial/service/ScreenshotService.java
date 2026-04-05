package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.kafka.screenshot.ScreenshotProducer;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import com.adventuretube.geospatial.repository.ScreenshotJobStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenshotService {
    private final AdventureTubeDataRepository adventureTubeDataRepository;
    private final ScreenshotJobStatusRepository screenshotJobStatusRepository;
    private final ScreenshotProducer screenshotProducer;

    public void deleteScreenshots(String youtubeContentID, String trackingId,AdventureTubeData adventureTubeData) {

        screenshotProducer.deleteScreenshotRequest(youtubeContentID, trackingId, adventureTubeData);
        log.info("All screenshots deleted reqeust has been sent for  {}", youtubeContentID);


    }

}
