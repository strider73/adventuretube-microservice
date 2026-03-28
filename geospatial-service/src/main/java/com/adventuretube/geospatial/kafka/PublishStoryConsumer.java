package com.adventuretube.geospatial.kafka;

import org.springframework.dao.DuplicateKeyException;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import com.adventuretube.geospatial.service.PublishStoryJobStatusService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublishStoryConsumer {
    private static final Logger logger = LoggerFactory.getLogger(PublishStoryConsumer.class);

    private final AdventureTubeDataService adventureTubeDataService;
    private final PublishStoryJobStatusService publishStoryJobStatusService;
    private final ObjectMapper objectMapper;

    private final Producer producer;

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
                publishStoryJobStatusService.markFailed(trackingId, "Unknown action: " + kafkaMessage.getAction());
            }
        }
    }

    private void handleSave(KafkaMessage kafkaMessage, String trackingId) {
        AdventureTubeData data = kafkaMessage.getData();
        if (data == null) {
            logger.error("SAVE action but data is null, trackingId={}", trackingId);
            publishStoryJobStatusService.markFailed(trackingId, "Data is null");
            return;
        }

        try {
            AdventureTubeData saved = adventureTubeDataService.save(data);
            logger.info("Saved AdventureTubeData: youtubeContentID={}", saved.getYoutubeContentID());
            int chaptersCount = saved.getChapters() != null ? saved.getChapters().size() : 0;
            int placesCount = saved.getPlaces() != null ? saved.getPlaces().size() : 0;
            publishStoryJobStatusService.markCompleted(trackingId, chaptersCount, placesCount);
            //trigger async screenshot generation
            producer.sendScreenshotRequest(data.getYoutubeContentID(), data);

        } catch (DuplicateKeyException e) {
            logger.warn("Duplicate youtubeContentID={}, skipping", data.getYoutubeContentID());
            publishStoryJobStatusService.markCompletedWithDuplicate(trackingId);
        } catch (Exception e) {
            logger.error("Failed to save AdventureTubeData: {}", e.getMessage(), e);
            publishStoryJobStatusService.markFailed(trackingId, e.getMessage());
        }
    }

    private void handleDelete(KafkaMessage kafkaMessage, String trackingId) {
        String youtubeContentId = kafkaMessage.getYoutubeContentId();
        String ownerEmail = kafkaMessage.getOwnerEmail();

        if (youtubeContentId == null || ownerEmail == null) {
            logger.error("DELETE action but missing youtubeContentId or ownerEmail, trackingId={}", trackingId);
            publishStoryJobStatusService.markFailed(trackingId, "Missing youtubeContentId or ownerEmail");
            return;
        }

        try {
            adventureTubeDataService.deleteByYoutubeContentIdAndOwnerEmail(youtubeContentId, ownerEmail);
            logger.info("Deleted AdventureTubeData: youtubeContentID={}", youtubeContentId);
            publishStoryJobStatusService.markCompleted(trackingId, 0, 0);
        } catch (Exception e) {
            logger.error("Failed to delete AdventureTubeData: {}", e.getMessage(), e);
            publishStoryJobStatusService.markFailed(trackingId, e.getMessage());
        }
    }
}
