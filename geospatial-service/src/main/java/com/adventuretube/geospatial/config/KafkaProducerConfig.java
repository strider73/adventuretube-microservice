package com.adventuretube.geospatial.config;

import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;

import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate(
            KafkaProperties kafkaProperties) {

        Map<String, Object> props = kafkaProperties.buildProducerProperties(null);

        SenderOptions<String, String> senderOptions = SenderOptions.<String, String>create(props)
                .withKeySerializer(new StringSerializer())
                .withValueSerializer(new StringSerializer());

        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }
}
