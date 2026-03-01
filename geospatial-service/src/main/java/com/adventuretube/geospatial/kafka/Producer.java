package com.adventuretube.geospatial.kafka;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Producer {
    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    private static final String TOPIC = "adventuretube-data";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendAdventureTubeData(AdventureTubeData data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            String key = data.getYoutubeContentID();
            logger.info("Publishing to Kafka topic={} key={}", TOPIC, key);
            kafkaTemplate.send(TOPIC, key, json);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize AdventureTubeData: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize AdventureTubeData", e);
        }
    }
}
