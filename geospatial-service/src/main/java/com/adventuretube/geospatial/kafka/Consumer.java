package com.adventuretube.geospatial.kafka;

import org.springframework.dao.DuplicateKeyException;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import com.adventuretube.geospatial.service.JobStatusService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    private final AdventureTubeDataService adventureTubeDataService;
    private final JobStatusService jobStatusService;
    private final ObjectMapper objectMapper;

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
                jobStatusService.markFailed(trackingId, "Unknown action: " + kafkaMessage.getAction());
            }
        }
    }

    private void handleSave(KafkaMessage kafkaMessage, String trackingId) {
        AdventureTubeData data = kafkaMessage.getData();
        if (data == null) {
            logger.error("SAVE action but data is null, trackingId={}", trackingId);
            jobStatusService.markFailed(trackingId, "Data is null");
            return;
        }

        try {
            AdventureTubeData saved = adventureTubeDataService.save(data);
            logger.info("Saved AdventureTubeData: youtubeContentID={}", saved.getYoutubeContentID());
            int chaptersCount = saved.getChapters() != null ? saved.getChapters().size() : 0;
            int placesCount = saved.getPlaces() != null ? saved.getPlaces().size() : 0;
            jobStatusService.markCompleted(trackingId, chaptersCount, placesCount);
        } catch (DuplicateKeyException e) {
            logger.warn("Duplicate youtubeContentID={}, skipping", data.getYoutubeContentID());
            jobStatusService.markCompletedWithDuplicate(trackingId);
        } catch (Exception e) {
            logger.error("Failed to save AdventureTubeData: {}", e.getMessage(), e);
            jobStatusService.markFailed(trackingId, e.getMessage());
        }
    }

    private void handleDelete(KafkaMessage kafkaMessage, String trackingId) {
        String youtubeContentId = kafkaMessage.getYoutubeContentId();
        String ownerEmail = kafkaMessage.getOwnerEmail();

        if (youtubeContentId == null || ownerEmail == null) {
            logger.error("DELETE action but missing youtubeContentId or ownerEmail, trackingId={}", trackingId);
            jobStatusService.markFailed(trackingId, "Missing youtubeContentId or ownerEmail");
            return;
        }

        try {
            adventureTubeDataService.deleteByYoutubeContentIdAndOwnerEmail(youtubeContentId, ownerEmail);
            logger.info("Deleted AdventureTubeData: youtubeContentID={}", youtubeContentId);
            jobStatusService.markCompleted(trackingId, 0, 0);
        } catch (Exception e) {
            logger.error("Failed to delete AdventureTubeData: {}", e.getMessage(), e);
            jobStatusService.markFailed(trackingId, e.getMessage());
        }
    }
}
