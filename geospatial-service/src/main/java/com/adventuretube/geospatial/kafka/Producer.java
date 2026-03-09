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

    public void sendAdventureTubeData(String trackingId, AdventureTubeData data) {
        KafkaMessage kafkaMessage = new KafkaMessage(trackingId, data);
        String json;
        try {
            json = objectMapper.writeValueAsString(kafkaMessage);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize KafkaMessage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize KafkaMessage", e);
        }

        String key = data.getYoutubeContentID();
        logger.info("Publishing to Kafka topic={} key={} trackingId={}", TOPIC, key, trackingId);

        kafkaTemplate.send(TOPIC, key, json)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to send to Kafka: topic={} key={} trackingId={}", TOPIC, key, trackingId, ex);
                    } else {
                        logger.info("Sent to Kafka: topic={} key={} trackingId={} offset={}",
                                TOPIC, key, trackingId, result.getRecordMetadata().offset());
                    }
                });
    }
}
