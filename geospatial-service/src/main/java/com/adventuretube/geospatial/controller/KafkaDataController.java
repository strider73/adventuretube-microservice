package com.adventuretube.geospatial.controller;


import com.adventuretube.geospatial.kafka.Producer;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping(value = "/geo/kafka")
public class KafkaDataController {

    private final Producer producer;
    @PostMapping(value = "/publish")
    public ResponseEntity<String> sendMessageToKafkaTopic(@RequestParam("message") String message) {
        this.producer.sendMessage(message);
        return ResponseEntity.ok("message :"+message+" has been sent successfully to kafka");
    }


    @PostMapping(value = "/publish/repeat")
    public ResponseEntity<String> sendMessageToKafkaTopicRepeatedly(@RequestParam("message") String message, @RequestParam("count") int count) {
        for (int i = 0; i < count; i++) {
            this.producer.sendMessage(message);
        }
        return ResponseEntity.ok("Message: " + message + " has been sent " + count + " times successfully to Kafka");
    }

}
