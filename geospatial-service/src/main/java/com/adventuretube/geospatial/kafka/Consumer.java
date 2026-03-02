package com.adventuretube.geospatial.kafka;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
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
public class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    private final AdventureTubeDataService adventureTubeDataService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "adventuretube-data", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        logger.info("Consumed message from adventuretube-data: {}",
                message.length() > 200 ? message.substring(0, 200) + "..." : message);

        AdventureTubeData data;
        try {
            data = objectMapper.readValue(message, AdventureTubeData.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize message, skipping: {}", e.getMessage());
            return;
        }

        try {
            AdventureTubeData saved = adventureTubeDataService.save(data);
            logger.info("Saved AdventureTubeData: youtubeContentID={}", saved.getYoutubeContentID());
        } catch (DuplicateKeyException e) {
            logger.warn("Duplicate youtubeContentID={}, skipping", data.getYoutubeContentID());
        }
    }
}
