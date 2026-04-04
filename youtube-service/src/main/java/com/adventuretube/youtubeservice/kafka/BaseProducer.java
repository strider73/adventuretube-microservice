package com.adventuretube.youtubeservice.kafka;


import com.adventuretube.youtubeservice.kafka.entity.KafkaMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public class BaseProducer {
    private static final Logger logger = LoggerFactory.getLogger(BaseProducer.class);
    protected final KafkaTemplate<String, String> kafkaTemplate;
    protected final ObjectMapper objectMapper;


    protected void sendToKafka(String topic, String key, String json,String logContext){
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


    protected String serializeMessage(KafkaMessage kafkaMessage){
        try {
            return objectMapper.writeValueAsString(kafkaMessage);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize KafkaMessage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize KafkaMessage", e);
        }
    }
}
