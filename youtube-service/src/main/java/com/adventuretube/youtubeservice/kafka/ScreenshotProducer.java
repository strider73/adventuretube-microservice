package com.adventuretube.youtubeservice.kafka;


import com.adventuretube.youtubeservice.model.entity.adventuretube.AdventureTubeData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ScreenshotProducer {

    private static final Logger logger = LoggerFactory.getLogger(ScreenshotProducer.class);
    private static final String SCREENSHOT_CREATE_TOPIC = "adventuretube-screenshots";
    private static final String SCREENSHOT_DELETE_TOPIC = "adventuretube-screenshots";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void returnScreenshotURL(String youtubeContentID, String trackingId, AdventureTubeData data) {
        KafkaMessage kafkaMessage = new KafkaMessage(trackingId, youtubeContentID, null,
                KafkaAction.GENERATE_SCREENSHOTS, data);
        String json = serializeMessage(kafkaMessage);
        sendToKafka(SCREENSHOT_CREATE_TOPIC, youtubeContentID, json, "youtubeContentID=" + youtubeContentID);
    }


    public void deleteScreenshotRequest(String youtubeContentID,String trackingId,AdventureTubeData data){
        KafkaMessage kafkaMessage = new KafkaMessage(trackingId, youtubeContentID, null, KafkaAction.DELETE_SCREENSHOTS, data);
        String json = serializeMessage(kafkaMessage);
        sendToKafka(SCREENSHOT_DELETE_TOPIC, youtubeContentID, json, "trackingId=" + trackingId);
    }
    private void sendToKafka(String topic, String key, String json,String logContext){
        logger.info("Publishing to Kafka: topic={} key={} {}", topic, key, logContext);
        kafkaTemplate.send(topic, key, json)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to send to Kafka: topic={} key={} {}", topic, key, logContext, ex);
                    } else {
                        logger.info("Sent to Kafka: topic={} key={} {} offset={}",
                                topic, key, logContext, result.getRecordMetadata().offset());
                    }
                });
    }


    private String serializeMessage(KafkaMessage kafkaMessage){
        try {
            return objectMapper.writeValueAsString(kafkaMessage);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize KafkaMessage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize KafkaMessage", e);
        }
    }
}
