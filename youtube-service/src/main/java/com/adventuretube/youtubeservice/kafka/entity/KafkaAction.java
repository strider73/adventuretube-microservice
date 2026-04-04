package com.adventuretube.youtubeservice.kafka.entity;

public enum KafkaAction {
    CREATE,
    UPDATE,
    DELETE ,
    //request
    GENERATE_SCREENSHOTS,
    DELETE_SCREENSHOTS,
    //result
    SCREENSHOTS_COMPLETED,
    SCREENSHOTS_DELETED

}
