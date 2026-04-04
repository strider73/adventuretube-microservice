package com.adventuretube.youtubeservice.kafka.entity;

import com.adventuretube.youtubeservice.kafka.entity.KafkaAction;
import com.adventuretube.youtubeservice.model.entity.adventuretube.AdventureTubeData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KafkaMessage {
    private String trackingId;
    private String youtubeContentId;
    private String ownerEmail;
    private KafkaAction action;

    private AdventureTubeData data;

}
