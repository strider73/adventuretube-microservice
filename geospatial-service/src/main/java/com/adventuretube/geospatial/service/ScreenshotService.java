package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.kafka.screenshot.ScreenshotProducer;
import com.adventuretube.geospatial.model.entity.ScreenshotJobStatus;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.model.entity.adventuretube.Chapter;
import com.adventuretube.geospatial.model.enums.ScreenshotJobStatusEnum;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import com.adventuretube.geospatial.repository.ScreenshotJobStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;



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
