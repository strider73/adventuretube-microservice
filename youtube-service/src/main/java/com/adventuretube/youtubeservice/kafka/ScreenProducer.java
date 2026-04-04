package com.adventuretube.youtubeservice.kafka;


import com.adventuretube.youtubeservice.kafka.entity.KafkaAction;
import com.adventuretube.youtubeservice.kafka.entity.KafkaMessage;
import com.adventuretube.youtubeservice.model.entity.adventuretube.AdventureTubeData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScreenProducer extends  BaseProducer {

    private static final Logger logger = LoggerFactory.getLogger(ScreenProducer.class);
    private static final String SCREENSHOT_RESULT_TOPIC = "adventuretube-screenshots-result";

    public ScreenProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper);
    }

    public void returnScreenshotURL(String youtubeContentID, String trackingId, AdventureTubeData data) {
        com.adventuretube.youtubeservice.kafka.entity.KafkaMessage kafkaMessage = new KafkaMessage(trackingId, youtubeContentID, null,
                KafkaAction.SCREENSHOTS_COMPLETED, data);
        String json = serializeMessage(kafkaMessage);
        sendToKafka(SCREENSHOT_RESULT_TOPIC, youtubeContentID, json, "youtubeContentID=" + youtubeContentID);
    }

    public void returnDeleteResult(String youtubeContentID, String trackingId, AdventureTubeData data) {
        com.adventuretube.youtubeservice.kafka.entity.KafkaMessage kafkaMessage = new KafkaMessage(trackingId, youtubeContentID, null,
                KafkaAction.SCREENSHOTS_DELETED, data);
        String json = serializeMessage(kafkaMessage);
        sendToKafka(SCREENSHOT_RESULT_TOPIC, youtubeContentID, json, "youtubeContentID=" + youtubeContentID);
    }
}
