package com.adventuretube.geospatial.kafka.story;

import com.adventuretube.geospatial.kafka.BaseProducer;
import com.adventuretube.geospatial.kafka.entity.KafkaAction;
import com.adventuretube.geospatial.kafka.entity.KafkaMessage;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class StoryProducer extends BaseProducer {
    private static final Logger logger = LoggerFactory.getLogger(StoryProducer.class);
    private static final String STORY_CREATE_TOPIC = "adventuretube-data";
    private static final String STORY_DELETE_TOPIC = "adventuretube-data";




    public StoryProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        super(kafkaTemplate, objectMapper);
    }

    public void sendAdventureTubeData(String trackingId, AdventureTubeData data) {
        KafkaMessage kafkaMessage = new KafkaMessage(trackingId, null, null, KafkaAction.CREATE, data);
        String json = serializeMessage(kafkaMessage);
        sendToKafka(STORY_CREATE_TOPIC, data.getYoutubeContentID(), json, "trackingId=" + trackingId);
    }

    public void deleteAdventureTubeData(String trackingId, String youtubeContentId, String ownerEmail) {
        KafkaMessage kafkaMessage = new KafkaMessage(trackingId, youtubeContentId, ownerEmail, KafkaAction.DELETE,
                null);
        String json = serializeMessage(kafkaMessage);
        sendToKafka(STORY_DELETE_TOPIC, youtubeContentId, json, "trackingId=" + trackingId);
    }



}
