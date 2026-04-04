package com.adventuretube.youtubeservice.kafka;

import com.adventuretube.youtubeservice.service.ScreenshotService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenshotConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ScreenshotConsumer.class);
    private final ObjectMapper objectMapper;
    private final ScreenshotService screenshotService;

    @KafkaListener(topics = "adventuretube-screenshots", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message){
        log.info("Consumed screenshot message: {}",
                message.length() > 200 ? message.substring(0, 200) + "..." : message);

        KafkaMessage kafkaMessage;

        try {
            kafkaMessage = objectMapper.readValue(message, KafkaMessage.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize screenshot message: {}", e.getMessage());
            return;
        }


        String trackingId = kafkaMessage.getTrackingId();
        if(trackingId == null){
            logger.error("Missing trackingId, skipping message");
            return;
        }


        switch(kafkaMessage.getAction()){
            case GENERATE_SCREENSHOTS -> handleGenerateScreenshots(kafkaMessage,trackingId);
            case DELETE_SCREENSHOTS -> handleDeleteScreenshots(kafkaMessage,trackingId);
            default -> {
                log.error("Unexpected action on screenshot topic: {}", kafkaMessage.getAction());
            }
        }
    }
    private void handleGenerateScreenshots(KafkaMessage kafkaMessage, String trackingId) {
        if (kafkaMessage.getData() == null || kafkaMessage.getData().getChapters() == null) {
            log.error("Screenshot message has no data or chapters");
            return;
        }

        String youtubeContentID = kafkaMessage.getYoutubeContentId();
        log.info("Starting screenshot generation for youtubeContentID={}", youtubeContentID);
        screenshotService.processScreenshotJob(youtubeContentID, kafkaMessage.getData().getChapters());


    }
    //Story consumer already check for adventuretubeData exist using youtubeContentId and data already added to kafkaMessage
    private void handleDeleteScreenshots(KafkaMessage kafkaMessage, String trackingId) {
        //double check the data and chapter exist
        if (kafkaMessage.getData() == null || kafkaMessage.getData().getChapters() == null) {
            log.error("Screenshot message has no data or chapters");
            return;
        }

        String youtubeContentID = kafkaMessage.getYoutubeContentId();
        log.info("Starting screenshot deletion for youtubeContentID={}", youtubeContentID);
        screenshotService.deleteScreenshots(youtubeContentID, kafkaMessage.getData());

    }
}
