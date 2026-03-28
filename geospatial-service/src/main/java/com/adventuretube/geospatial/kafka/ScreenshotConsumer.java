package com.adventuretube.geospatial.kafka;

import com.adventuretube.geospatial.service.ScreenshotService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenshotConsumer {

    private final ScreenshotService screenshotService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "adventuretube-screenshots", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        log.info("Consumed screenshot message: {}",
                message.length() > 200 ? message.substring(0, 200) + "..." : message);

        KafkaMessage kafkaMessage;
        try {
            kafkaMessage = objectMapper.readValue(message, KafkaMessage.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize screenshot message: {}", e.getMessage());
            return;
        }

        if (kafkaMessage.getAction() != KafkaAction.GENERATE_SCREENSHOTS) {
            log.error("Unexpected action on screenshot topic: {}", kafkaMessage.getAction());
            return;
        }

        if (kafkaMessage.getData() == null || kafkaMessage.getData().getChapters() == null) {
            log.error("Screenshot message has no data or chapters");
            return;
        }

        String youtubeContentID = kafkaMessage.getYoutubeContentId();
        log.info("Starting screenshot generation for youtubeContentID={}", youtubeContentID);

        screenshotService.processScreenshotJob(youtubeContentID, kafkaMessage.getData().getChapters());
    }
}
