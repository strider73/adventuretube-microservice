package com.adventuretube.geospatial.kafka;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class Producer {
    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    private static final String TOPIC = "adventuretube-data";

    private final ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate;
    private final ObjectMapper objectMapper;

    public Mono<Void> sendAdventureTubeData(AdventureTubeData data) {
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize AdventureTubeData: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Failed to serialize AdventureTubeData", e));
        }

        String key = data.getYoutubeContentID();
        logger.info("Publishing to Kafka topic={} key={}", TOPIC, key);

        return reactiveKafkaProducerTemplate.send(TOPIC, key, json)
                .doOnSuccess(result -> logger.info("Sent to Kafka: topic={} key={} offset={}",
                        TOPIC, key, result.recordMetadata().offset()))
                .then();
    }
}
