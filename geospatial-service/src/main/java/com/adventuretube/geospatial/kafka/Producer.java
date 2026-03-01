package com.adventuretube.geospatial.kafka;

import brave.Span;
import brave.Tracer;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class Producer {
    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    private static final String TOPIC = "adventuretube-data";

    private final ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public Mono<Void> sendAdventureTubeData(AdventureTubeData data) {
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize AdventureTubeData: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Failed to serialize AdventureTubeData", e));
        }

        String key = data.getYoutubeContentID();
        logger.info("Publishing to Kafka topic={} key={}", TOPIC, key);

        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, json);
        injectTraceHeaders(record);

        return reactiveKafkaProducerTemplate.send(record)
                .doOnSuccess(result -> logger.info("Sent to Kafka: topic={} key={} offset={}",
                        TOPIC, key, result.recordMetadata().offset()))
                .then();
    }

    private void injectTraceHeaders(ProducerRecord<String, String> record) {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            logger.debug("No active span — skipping B3 header injection");
            return;
        }
        brave.propagation.TraceContext ctx = currentSpan.context();
        record.headers().add("X-B3-TraceId", ctx.traceIdString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("X-B3-SpanId", ctx.spanIdString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("X-B3-Sampled", (ctx.sampled() != null && ctx.sampled() ? "1" : "0").getBytes(StandardCharsets.UTF_8));
        if (ctx.parentIdString() != null) {
            record.headers().add("X-B3-ParentSpanId", ctx.parentIdString().getBytes(StandardCharsets.UTF_8));
        }
        logger.debug("Injected B3 headers: traceId={} spanId={}", ctx.traceIdString(), ctx.spanIdString());
    }
}
