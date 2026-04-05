package com.adventuretube.geospatial.kafka.screenshot;


import com.adventuretube.geospatial.kafka.entity.KafkaMessage;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import com.adventuretube.geospatial.service.jobstatus.ScreenshotJobStatusService;
import com.adventuretube.geospatial.service.jobstatus.StoryJobStatusService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScreenshotConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ScreenshotConsumer.class);

    private final AdventureTubeDataService adventureTubeDataService;
    private final ObjectMapper objectMapper;
    private final StoryJobStatusService storyJobStatusService;
    private final ScreenshotJobStatusService screenshotJobStatusService;


    @KafkaListener(topics = "adventuretube-screenshots-result", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        logger.info("Consumed message from adventuretube-screenshots-result: {}",
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
            case SCREENSHOTS_COMPLETED -> storeScreenshotData(kafkaMessage);
            case SCREENSHOTS_DELETED -> deleteAdventureTubeData(kafkaMessage, trackingId);
            default -> {
                logger.error("Unknown action: {}, skipping", kafkaMessage.getAction());
            }
        }
    }

    // Screenshot save flow (end of sequence):
    // 1. StoryConsumer.handleSave() → saves AdventureTubeData → marks StoryJobStatus COMPLETED (SSE to iOS)
    // 2. StoryConsumer creates ScreenshotJobStatus PENDING → sends GENERATE_SCREENSHOTS to youtube-service via Kafka
    // 3. youtube-service runs yt-dlp + ffmpeg → uploads to S3 → sends SCREENSHOTS_COMPLETED back via Kafka
    // 4. This method: updates chapter screenshotUrls in MongoDB → marks ScreenshotJobStatus COMPLETED
    private void storeScreenshotData(KafkaMessage kafkaMessage) {
        AdventureTubeData data = kafkaMessage.getData();
        String youtubeContentId = kafkaMessage.getYoutubeContentId();
        if (data == null) {
            logger.error("SCREENSHOTS_COMPLETED but data is null, youtubeContentId={}", youtubeContentId);
            screenshotJobStatusService.markFailed(youtubeContentId, "Screenshot result data is null");
            return;
        }
        try {
            adventureTubeDataService.findByYoutubeContentID(youtubeContentId)
                    .ifPresent(existing -> {
                        existing.setChapters(data.getChapters());
                        adventureTubeDataService.save(existing);
                    });

            int completedChapters = (int) data.getChapters().stream()
                    .filter(ch -> ch.getScreenshotUrl() != null)
                    .count();
            screenshotJobStatusService.markCompleted(youtubeContentId, completedChapters);
            logger.info("Updated screenshot URLs and marked ScreenshotJobStatus COMPLETED: youtubeContentID={}, completedChapters={}",
                    youtubeContentId, completedChapters);

        } catch (DuplicateKeyException e) {
            logger.warn("Duplicate youtubeContentID={}, skipping", data.getYoutubeContentID());
        } catch (Exception e) {
            logger.error("Failed to save screenshot results: {}", e.getMessage(), e);
            screenshotJobStatusService.markFailed(youtubeContentId, e.getMessage());
        }
    }

    // Delete flow (end of sequence):
    // 1. StoryConsumer.handleDelete() → validates ownership → creates StoryJobStatus PENDING
    // 2. ScreenshotProducer sends DELETE_SCREENSHOTS to youtube-service via Kafka
    // 3. youtube-service deletes images from S3 → sends SCREENSHOTS_DELETED back via Kafka
    // 4. This method: deletes AdventureTubeData from MongoDB → marks StoryJobStatus COMPLETED
    //
    // storyJobStatusService.markCompleted() here closes the delete trackingId loop —
    // telling iOS "your delete request is done"
    private void deleteAdventureTubeData(KafkaMessage kafkaMessage, String trackingId) {
        try {
            adventureTubeDataService.findByYoutubeContentID(kafkaMessage.getYoutubeContentId())
                    .ifPresent(existing -> {
                        adventureTubeDataService.deleteByYoutubeContentId(kafkaMessage.getYoutubeContentId());
                    });
            // 1. Mark completed(not much important) + send SSE to iOS (must be done)
            storyJobStatusService.markCompleted(trackingId, 0, 0);
            //clean up job status records here can  make a jobStatus not found error


        } catch (Exception e) {
            logger.error("Failed to delete AdventureTubeData: {}", e.getMessage(), e);
        }
    }

}
