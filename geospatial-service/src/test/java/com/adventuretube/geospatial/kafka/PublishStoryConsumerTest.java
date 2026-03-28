package com.adventuretube.geospatial.kafka;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import com.adventuretube.geospatial.service.PublishStoryJobStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishStoryConsumerTest {

    @Mock
    private AdventureTubeDataService adventureTubeDataService;

    @Mock
    private PublishStoryJobStatusService publishStoryJobStatusService;

    @Mock
    private Producer producer;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PublishStoryConsumer publishStoryConsumer;

    private AdventureTubeData testData;

    @BeforeEach
    void setUp() {
        testData = new AdventureTubeData();
        testData.setYoutubeContentID("yt-test-123");
        testData.setYoutubeTitle("Test Video");
        testData.setChapters(List.of());
        testData.setPlaces(List.of());
    }

    @Test
    void consume_shouldSaveAndMarkCompleted_whenNewData() throws Exception {
        AdventureTubeData saved = new AdventureTubeData();
        saved.setYoutubeContentID("yt-test-123");
        saved.setChapters(List.of());
        saved.setPlaces(List.of());

        when(adventureTubeDataService.save(any())).thenReturn(saved);

        KafkaMessage kafkaMessage = new KafkaMessage("tracking-123", "yt-test-123", null, KafkaAction.CREATE, testData);
        String json = objectMapper.writeValueAsString(kafkaMessage);

        publishStoryConsumer.consume(json);

        verify(adventureTubeDataService).save(any(AdventureTubeData.class));
        verify(publishStoryJobStatusService).markCompleted("tracking-123", 0, 0);
        verify(publishStoryJobStatusService, never()).markCompletedWithDuplicate(anyString());
        verify(publishStoryJobStatusService, never()).markFailed(anyString(), anyString());
    }

    @Test
    void consume_shouldMarkDuplicate_whenDuplicateKeyException() throws Exception {
        when(adventureTubeDataService.save(any()))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate key"));

        KafkaMessage kafkaMessage = new KafkaMessage("tracking-dup", "yt-test-123", null, KafkaAction.CREATE, testData);
        String json = objectMapper.writeValueAsString(kafkaMessage);

        publishStoryConsumer.consume(json);

        verify(adventureTubeDataService).save(any(AdventureTubeData.class));
        verify(publishStoryJobStatusService).markCompletedWithDuplicate("tracking-dup");
        verify(publishStoryJobStatusService, never()).markCompleted(anyString(), anyInt(), anyInt());
        verify(publishStoryJobStatusService, never()).markFailed(anyString(), anyString());
    }

    @Test
    void consume_shouldMarkFailed_whenUnexpectedException() throws Exception {
        when(adventureTubeDataService.save(any()))
                .thenThrow(new RuntimeException("MongoDB connection lost"));

        KafkaMessage kafkaMessage = new KafkaMessage("tracking-fail", "yt-test-123", null, KafkaAction.CREATE, testData);
        String json = objectMapper.writeValueAsString(kafkaMessage);

        publishStoryConsumer.consume(json);

        verify(adventureTubeDataService).save(any(AdventureTubeData.class));
        verify(publishStoryJobStatusService).markFailed("tracking-fail", "MongoDB connection lost");
        verify(publishStoryJobStatusService, never()).markCompleted(anyString(), anyInt(), anyInt());
        verify(publishStoryJobStatusService, never()).markCompletedWithDuplicate(anyString());
    }

    @Test
    void consume_shouldSkipProcessing_whenKafkaMessageHasNullTrackingIdAndData() throws Exception {
        // KafkaMessage with trackingId=null, data has value → fails the && check
        // Falls back to parsing as raw AdventureTubeData which fails (unknown fields).
        // PublishStoryConsumer logs error and returns without saving or updating job status.
        String json = "{\"trackingId\":null,\"data\":" + objectMapper.writeValueAsString(testData) + "}";

        publishStoryConsumer.consume(json);

        verify(adventureTubeDataService, never()).save(any());
        verify(publishStoryJobStatusService, never()).markCompletedWithDuplicate(anyString());
        verify(publishStoryJobStatusService, never()).markCompleted(anyString(), anyInt(), anyInt());
        verify(publishStoryJobStatusService, never()).markFailed(anyString(), anyString());
    }

    @Test
    void consume_shouldSkip_whenInvalidJson() {
        publishStoryConsumer.consume("not valid json {{{}}}");

        verify(adventureTubeDataService, never()).save(any());
        verify(publishStoryJobStatusService, never()).markCompleted(anyString(), anyInt(), anyInt());
        verify(publishStoryJobStatusService, never()).markCompletedWithDuplicate(anyString());
        verify(publishStoryJobStatusService, never()).markFailed(anyString(), anyString());
    }

    @Test
    void consume_shouldCountChaptersAndPlaces_whenCompleted() throws Exception {
        AdventureTubeData saved = new AdventureTubeData();
        saved.setYoutubeContentID("yt-test-123");
        saved.setChapters(List.of(
                new com.adventuretube.geospatial.model.entity.adventuretube.Chapter(),
                new com.adventuretube.geospatial.model.entity.adventuretube.Chapter(),
                new com.adventuretube.geospatial.model.entity.adventuretube.Chapter()
        ));
        saved.setPlaces(List.of(
                new com.adventuretube.geospatial.model.entity.adventuretube.Place(),
                new com.adventuretube.geospatial.model.entity.adventuretube.Place()
        ));

        when(adventureTubeDataService.save(any())).thenReturn(saved);

        KafkaMessage kafkaMessage = new KafkaMessage("tracking-counts", "yt-test-123", null, KafkaAction.CREATE, testData);
        String json = objectMapper.writeValueAsString(kafkaMessage);

        publishStoryConsumer.consume(json);

        verify(publishStoryJobStatusService).markCompleted("tracking-counts", 3, 2);
    }
}
