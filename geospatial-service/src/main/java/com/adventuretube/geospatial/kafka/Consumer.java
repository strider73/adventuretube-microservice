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

        // Try KafkaMessage envelope first, fall back to raw AdventureTubeData
        String trackingId = null;
        AdventureTubeData data;
        try {
            KafkaMessage kafkaMessage = objectMapper.readValue(message, KafkaMessage.class);
            if (kafkaMessage.getTrackingId() != null && kafkaMessage.getData() != null) {
                trackingId = kafkaMessage.getTrackingId();
                data = kafkaMessage.getData();
            } else {
                // Fallback: raw AdventureTubeData (old format, no tracking)
                data = objectMapper.readValue(message, AdventureTubeData.class);
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize message, skipping: {}", e.getMessage());
            return;
        }

        try {
            AdventureTubeData saved = adventureTubeDataService.save(data);
            logger.info("Saved AdventureTubeData: youtubeContentID={}", saved.getYoutubeContentID());

            if (trackingId != null) {
                int chaptersCount = saved.getChapters() != null ? saved.getChapters().size() : 0;
                int placesCount = saved.getPlaces() != null ? saved.getPlaces().size() : 0;
                jobStatusService.markCompleted(trackingId, chaptersCount, placesCount);
            }
        } catch (DuplicateKeyException e) {
            logger.warn("Duplicate youtubeContentID={}, skipping", data.getYoutubeContentID());
            if (trackingId != null) {
                jobStatusService.markDuplicate(trackingId);
            }
        } catch (Exception e) {logger.error("Failed to save AdventureTubeData: {}", e.getMessage(), e);
            if (trackingId != null) {
                jobStatusService.markFailed(trackingId, e.getMessage());
            }
        }
    }
}
