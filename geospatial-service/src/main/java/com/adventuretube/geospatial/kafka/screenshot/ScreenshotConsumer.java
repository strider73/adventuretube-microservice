package com.adventuretube.geospatial.kafka.screenshot;


import com.adventuretube.geospatial.kafka.entity.KafkaMessage;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import com.adventuretube.geospatial.service.JobStatusService;
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
public class ScreenshotConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ScreenshotConsumer.class);

    private final AdventureTubeDataService adventureTubeDataService;
    private final ObjectMapper objectMapper;
    private final JobStatusService jobStatusService;


    @KafkaListener(topics = "adventuretube-screenshots-result", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        logger.info("Consumed message from adventuretube-screenshots-result: {}",
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
            case SCREENSHOTS_COMPLETED -> handleSave(kafkaMessage, trackingId);
            case SCREENSHOTS_DELETED -> handleDelete(kafkaMessage, trackingId);
            default -> {
                logger.error("Unknown action: {}, skipping", kafkaMessage.getAction());
            }
        }
    }


    private void handleSave(KafkaMessage kafkaMessage, String trackingId) {
        AdventureTubeData data = kafkaMessage.getData();
        if (data == null) {
            logger.error("SAVE action but data is null, trackingId={}", trackingId);
            return;
        }
        //TODO: handle save process currently use entire adventuretube-data instead chapter only
        try {
            //TODO: need a update process instead save
            adventureTubeDataService.findByYoutubeContentID(kafkaMessage.getYoutubeContentId())
                    .ifPresent(existing -> {
                        existing.setChapters(data.getChapters());
                        adventureTubeDataService.save(existing);
                    });


            logger.info("Update AdventureTubeData ScreenShot URL : youtubeContentID={}", kafkaMessage.getYoutubeContentId());


        } catch (DuplicateKeyException e) {
            logger.warn("Duplicate youtubeContentID={}, skipping", data.getYoutubeContentID());
        } catch (Exception e) {
            logger.error("Failed to save AdventureTubeData: {}", e.getMessage(), e);
        }
    }

    private  void handleDelete(KafkaMessage kafkaMessage, String trackingId) {
        //Deleting image from S3 has been completed by youtube-service
        //Delete Story from mongo now
        try {
            //find AdventureTubeData by youtubeContentID
            adventureTubeDataService.findByYoutubeContentID(kafkaMessage.getYoutubeContentId())
                    .ifPresent(existing -> {
                        adventureTubeDataService.deleteByYoutubeContentId(kafkaMessage.getYoutubeContentId());
                    });
            //update job status to completed
            AdventureTubeData adventureTubeData = kafkaMessage.getData();
            jobStatusService.markCompleted(trackingId,0,0);

        } catch (Exception e) {
            logger.error("Failed to delete AdventureTubeData: {}", e.getMessage(), e);
        }
    }

}
