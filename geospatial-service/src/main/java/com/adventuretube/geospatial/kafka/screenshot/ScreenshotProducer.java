package com.adventuretube.geospatial.kafka.screenshot;


import com.adventuretube.geospatial.kafka.BaseProducer;
import com.adventuretube.geospatial.kafka.entity.KafkaAction;
import com.adventuretube.geospatial.kafka.entity.KafkaMessage;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class ScreenshotProducer extends BaseProducer {
    private static final String SCREENSHOT_CREATE_TOPIC = "adventuretube-screenshots";
    private static final String SCREENSHOT_DELETE_TOPIC = "adventuretube-screenshots";

    public ScreenshotProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper);
    }


    public void sendScreenshotRequest(String youtubeContentID,String trackingId, AdventureTubeData data) {
        KafkaMessage kafkaMessage = new KafkaMessage(trackingId, youtubeContentID, null,
                KafkaAction.GENERATE_SCREENSHOTS, data);
        String json = serializeMessage(kafkaMessage);
        sendToKafka(SCREENSHOT_CREATE_TOPIC, youtubeContentID, json, "youtubeContentID=" + youtubeContentID);
    }



    //This request will be listened from youtube-service not the geospatial-service
    //because topic name of adventuretube-screenshots  and kafka action of DELETE_SCREENSHOTS
    public void deleteScreenshotRequest(String youtubeContentID,String trackingId,AdventureTubeData data){
        KafkaMessage kafkaMessage = new KafkaMessage(trackingId, youtubeContentID, null, KafkaAction.DELETE_SCREENSHOTS, data);
        String json = serializeMessage(kafkaMessage);
        sendToKafka(SCREENSHOT_DELETE_TOPIC, youtubeContentID, json, "trackingId=" + trackingId);
    }
}
