package com.adventuretube.geospatial.kafka.story;

import com.adventuretube.geospatial.exceptions.DataNotFoundException;
import com.adventuretube.geospatial.exceptions.OwnershipMismatchException;
import com.adventuretube.geospatial.exceptions.code.GeoErrorCode;
import com.adventuretube.geospatial.kafka.entity.KafkaMessage;
import com.adventuretube.geospatial.kafka.screenshot.ScreenshotProducer;
import com.adventuretube.geospatial.repository.ScreenshotJobStatusRepository;
import com.adventuretube.geospatial.model.entity.jobstatus.ScreenshotJobStatus;
import com.adventuretube.geospatial.service.jobstatus.ScreenshotJobStatusService;
import com.adventuretube.geospatial.service.jobstatus.StoryJobStatusService;
import com.adventuretube.geospatial.service.ScreenshotService;
import org.springframework.dao.DuplicateKeyException;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoryConsumer {
    private static final Logger logger = LoggerFactory.getLogger(StoryConsumer.class);

    private final AdventureTubeDataService adventureTubeDataService;
    private final ScreenshotService screenshotService;
    private final StoryJobStatusService storyJobStatusService;
    private final ScreenshotJobStatusRepository screenshotJobStatusRepository;
    private final ObjectMapper objectMapper;

    private final StoryProducer storyProducer;
    private final ScreenshotProducer screenshotProducer;
    private final ScreenshotJobStatusService screenshotJobStatusService;

    @KafkaListener(topics = "adventuretube-data", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        logger.info("Consumed message from adventuretube-data: {}",
                message.length() > 200 ? message.substring(0, 200) + "..." : message);

        KafkaMessage kafkaMessage;
        try {
            kafkaMessage = objectMapper.readValue(message, KafkaMessage.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize message, skipping: {}", e.getMessage());
            return;
        }

        String trackingId = kafkaMessage.getTrackingId();
        if (trackingId == null) {
            logger.error("Missing trackingId, skipping message");
            return;
        }

        switch (kafkaMessage.getAction()) {
            case CREATE -> handleSave(kafkaMessage, trackingId);
            case DELETE -> handleDelete(kafkaMessage, trackingId);
            default -> {
                logger.error("Unknown action: {}, skipping", kafkaMessage.getAction());
                storyJobStatusService.markFailed(trackingId, "Unknown action: " + kafkaMessage.getAction());
            }
        }
    }

    private void handleSave(KafkaMessage kafkaMessage, String trackingId) {
        AdventureTubeData data = kafkaMessage.getData();
        if (data == null) {
            logger.error("SAVE action but data is null, trackingId={}", trackingId);
            storyJobStatusService.markFailed(trackingId, "Data is null");
            return;
        }

        try {
            //adventureTubeData get saved in here and return saved object
            AdventureTubeData saved = adventureTubeDataService.save(data);
            logger.info("Saved AdventureTubeData: youtubeContentID={}", saved.getYoutubeContentID());
            int chaptersCount = saved.getChapters() != null ? saved.getChapters().size() : 0;
            int placesCount = saved.getPlaces() != null ? saved.getPlaces().size() : 0;
            storyJobStatusService.markCompleted(trackingId, chaptersCount, placesCount);


            // Trigger async screenshot generation.
            // ScreenshotJobStatus is created here; iOS will need to poll for its progress separately.
            logger.info("############ScreenShot process for trackingId={}, and youtubeContentId={}  start###############",trackingId,data.getYoutubeContentID());
            screenshotJobStatusService.createPendingJob(trackingId, data.getYoutubeContentID());

            screenshotProducer.sendScreenshotRequest(data.getYoutubeContentID(), trackingId, data);

        } catch (DuplicateKeyException e) {
            logger.warn("Duplicate youtubeContentID={}, skipping", data.getYoutubeContentID());
            storyJobStatusService.markCompletedWithDuplicate(trackingId);
        } catch (Exception e) {
            logger.error("Failed to save AdventureTubeData: {}", e.getMessage(), e);
            storyJobStatusService.markFailed(trackingId, e.getMessage());
        }
    }

    //deletion process will be one unified process for both delete screenshot and delete database
    //1. check the ownership of story
    //2. call the screenshot consumer for deletion
    //3. delete database
    //4. mark the job as completed
    private void handleDelete(KafkaMessage kafkaMessage, String trackingId) {
        String youtubeContentId = kafkaMessage.getYoutubeContentId();
        String ownerEmail = kafkaMessage.getOwnerEmail();

        if (youtubeContentId == null || ownerEmail == null) {
            logger.error("DELETE action but missing youtubeContentId or ownerEmail, trackingId={}", trackingId);
            storyJobStatusService.markFailed(trackingId, "youtubeContentId or email is null");
            return;
        }

        try {
            //check the ownership of story
            AdventureTubeData adventureTubeData = adventureTubeDataService.findByYoutubeContentID(youtubeContentId)
                    .map(data -> {
                        if (!data.getOwnerEmail().equals(ownerEmail)) {
                            logger.error("Unauthorized: ownerEmail does not match");
                            throw new OwnershipMismatchException(GeoErrorCode.OWNERSHIP_MISMATCH);
                        }
                        return data;
                    }).orElseThrow(() -> {
                        logger.warn("Data not found for youtubeContentId: {}", youtubeContentId);
                        return new DataNotFoundException(GeoErrorCode.DATA_NOT_FOUND);
                    });

            //TODO: Deleting image from S3 need to be implemented from  youtube-service  completed at geospatial-service

            //This will be the request point to create producer for deleting request that will be listened from youtube-service
            screenshotService.deleteScreenshots(youtubeContentId,trackingId,adventureTubeData );


        } catch (Exception e) {
            logger.error("Failed to delete: {}", e.getMessage(), e);
            storyJobStatusService.markFailed(trackingId, e.getMessage());
        }






    }
}
