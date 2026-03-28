package com.adventuretube.geospatial.integration;

import com.adventuretube.geospatial.model.entity.PublishStoryJobStatus;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.model.entity.adventuretube.Chapter;
import com.adventuretube.geospatial.model.entity.adventuretube.Location;
import com.adventuretube.geospatial.model.entity.adventuretube.Place;
import com.adventuretube.geospatial.model.enums.PublishStoryJobStatusEnum;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import com.adventuretube.geospatial.repository.PublishStoryJobStatusRepository;
import com.adventuretube.geospatial.service.PublishStoryJobStatusService;
import com.adventuretube.geospatial.kafka.Producer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Job Status tracking + SSE flow.
 * Uses real MongoDB, mocks Kafka Producer (same pattern as AdventureTubeDataFullStackIT).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class PublishStoryJobStatusFullFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PublishStoryJobStatusRepository publishStoryJobStatusRepository;

    @Autowired
    private AdventureTubeDataRepository adventureTubeDataRepository;

    @Autowired
    private PublishStoryJobStatusService publishStoryJobStatusService;

    @Autowired
    private ObjectMapper objectMapper;

    /** Mock Kafka Producer to avoid requiring a Kafka broker */
    @MockitoBean
    private Producer kafkaProducer;

    private final List<String> createdJobIds = new ArrayList<>();
    private final List<String> createdDataIds = new ArrayList<>();
    private final String testRunId = UUID.randomUUID().toString().substring(0, 8);

    @AfterEach
    void cleanup() {
        if (!createdJobIds.isEmpty()) {
            publishStoryJobStatusRepository.deleteAllById(createdJobIds);
        }
        if (!createdDataIds.isEmpty()) {
            adventureTubeDataRepository.deleteAllById(createdDataIds);
        }
        createdJobIds.clear();
        createdDataIds.clear();
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private String testYoutubeId(String suffix) {
        return "TEST_JOB_" + testRunId + "_" + suffix;
    }

    private Location createLocation(double lng, double lat) {
        Location loc = new Location();
        loc.setType("Point");
        loc.setCoordinates(new double[]{lng, lat});
        return loc;
    }

    private Place createPlace(String name, double lng, double lat) {
        return new Place(
                List.of("food", "travel"),
                120L,
                createLocation(lng, lat),
                "PLACE_" + UUID.randomUUID().toString().substring(0, 8),
                name
        );
    }

    private Chapter createChapter(String youtubeId, Place place) {
        return new Chapter(
                place,
                youtubeId,
                60L,
                List.of("adventure", "food")
        );
    }

    private AdventureTubeData createTestData(String youtubeIdSuffix) {
        String ytId = testYoutubeId(youtubeIdSuffix);
        Place place = createPlace("Test Place", 126.978, 37.566);
        Chapter chapter = createChapter(ytId, place);

        return new AdventureTubeData(
                List.of("travel"),
                List.of(place),
                "3 days",
                "Test description for " + ytId,
                "Test Title " + ytId,
                List.of(chapter),
                "video",
                "CORE_" + testRunId,
                ytId,
                "test@example.com"
        );
    }

    // ── POST /geo/save → creates PENDING job ─────────────────────────

    @Test
    void save_shouldCreatePendingJobInMongoDB() throws Exception {
        AdventureTubeData input = createTestData("pending");

        MvcResult result = mockMvc.perform(post("/geo/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.youtubeContentID").value(testYoutubeId("pending")))
                .andReturn();

        // Extract trackingId and verify it's in MongoDB
        String responseJson = result.getResponse().getContentAsString();
        String trackingId = objectMapper.readTree(responseJson).at("/data/trackingId").asText();

        Optional<PublishStoryJobStatus> job = publishStoryJobStatusRepository.findByTrackingId(trackingId);
        assertThat(job).isPresent();
        assertThat(job.get().getStatus()).isEqualTo(PublishStoryJobStatusEnum.PENDING);
        createdJobIds.add(job.get().getId());
    }

    // ── GET /geo/status/{trackingId} → REST fallback ─────────────────

    @Test
    void getStatus_shouldReturnPendingJob() throws Exception {
        String trackingId = UUID.randomUUID().toString();
        PublishStoryJobStatus job = publishStoryJobStatusService.createPendingJob(trackingId, testYoutubeId("restPending"));
        createdJobIds.add(job.getId());

        mockMvc.perform(get("/geo/status/{trackingId}", trackingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingId").value(trackingId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getStatus_shouldReturnCompletedJob() throws Exception {
        String trackingId = UUID.randomUUID().toString();
        PublishStoryJobStatus job = publishStoryJobStatusService.createPendingJob(trackingId, testYoutubeId("restCompleted"));
        createdJobIds.add(job.getId());

        publishStoryJobStatusService.markCompleted(trackingId, 3, 2);

        mockMvc.perform(get("/geo/status/{trackingId}", trackingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.chaptersCount").value(3))
                .andExpect(jsonPath("$.data.placesCount").value(2));
    }

    @Test
    void getStatus_shouldReturnDuplicateJob() throws Exception {
        String trackingId = UUID.randomUUID().toString();
        PublishStoryJobStatus job = publishStoryJobStatusService.createPendingJob(trackingId, testYoutubeId("restDup"));
        createdJobIds.add(job.getId());

        publishStoryJobStatusService.markCompletedWithDuplicate(trackingId);

        mockMvc.perform(get("/geo/status/{trackingId}", trackingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DUPLICATED"));
    }

    @Test
    void getStatus_shouldReturn404ForUnknownTrackingId() throws Exception {
        mockMvc.perform(get("/geo/status/{trackingId}", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("JOB_NOT_FOUND"));
    }

    // ── GET /geo/status/stream/{trackingId} → SSE ────────────────────

    @Test
    void sseStream_shouldReturnImmediatelyForTerminalStatus() throws Exception {
        String trackingId = UUID.randomUUID().toString();
        PublishStoryJobStatus job = publishStoryJobStatusService.createPendingJob(trackingId, testYoutubeId("sseTerminal"));
        createdJobIds.add(job.getId());

        publishStoryJobStatusService.markCompleted(trackingId, 5, 3);

        MvcResult result = mockMvc.perform(get("/geo/status/stream/{trackingId}", trackingId)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andReturn();

        String sseContent = result.getResponse().getContentAsString();
        assertThat(sseContent).contains("event:job-status");
        assertThat(sseContent).contains("COMPLETED");
    }

    @Test
    void sseStream_shouldPushCompletedEventWhenKafkaConsumerFinishes() throws Exception {
        // Step 1: Create a PENDING job (simulates what POST /geo/save does)
        String trackingId = UUID.randomUUID().toString();
        PublishStoryJobStatus job = publishStoryJobStatusService.createPendingJob(trackingId, testYoutubeId("ssePush"));
        createdJobIds.add(job.getId());

        // Step 2: Open SSE connection asynchronously (simulates iOS calling GET /status/stream)
        MvcResult asyncResult = mockMvc.perform(get("/geo/status/stream/{trackingId}", trackingId)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Step 3: Simulate Kafka consumer finishing — this calls sseEmitterManager.send()
        publishStoryJobStatusService.markCompleted(trackingId, 4, 2);

        // Step 4: Wait for async dispatch and verify SSE events
        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:job-status")))
                .andExpect(content().string(containsString("PENDING")))
                .andExpect(content().string(containsString("COMPLETED")));
    }

    @Test
    void sseStream_shouldPushDuplicateEventWhenKafkaConsumerDetectsDuplicate() throws Exception {
        String trackingId = UUID.randomUUID().toString();
        PublishStoryJobStatus job = publishStoryJobStatusService.createPendingJob(trackingId, testYoutubeId("sseDup"));
        createdJobIds.add(job.getId());

        MvcResult asyncResult = mockMvc.perform(get("/geo/status/stream/{trackingId}", trackingId)
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Simulate Kafka consumer detecting duplicate
        publishStoryJobStatusService.markCompletedWithDuplicate(trackingId);

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DUPLICATED")));
    }

    @Test
    void sseStream_shouldReturn404ForUnknownTrackingId() throws Exception {
        mockMvc.perform(get("/geo/status/stream/{trackingId}", UUID.randomUUID().toString())
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isNotFound());
    }

    // ── PublishStoryJobStatusService unit-level checks via integration context ────

    @Test
    void markFailed_shouldSetErrorMessage() {
        String trackingId = UUID.randomUUID().toString();
        PublishStoryJobStatus job = publishStoryJobStatusService.createPendingJob(trackingId, testYoutubeId("failed"));
        createdJobIds.add(job.getId());

        publishStoryJobStatusService.markFailed(trackingId, "Connection refused");

        Optional<PublishStoryJobStatus> updated = publishStoryJobStatusRepository.findByTrackingId(trackingId);
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo(PublishStoryJobStatusEnum.FAILED);
        assertThat(updated.get().getErrorMessage()).isEqualTo("Connection refused");
    }

    @Test
    void createPendingJob_shouldSetExpireAtForTTL() {
        String trackingId = UUID.randomUUID().toString();
        PublishStoryJobStatus job = publishStoryJobStatusService.createPendingJob(trackingId, testYoutubeId("ttl"));
        createdJobIds.add(job.getId());

        assertThat(job.getExpireAt()).isNotNull();
        assertThat(job.getCreatedAt()).isNotNull();
        assertThat(job.getUpdatedAt()).isNotNull();
    }
}
