package com.adventuretube.geospatial.integration;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.model.entity.adventuretube.Chapter;
import com.adventuretube.geospatial.model.entity.adventuretube.Location;
import com.adventuretube.geospatial.model.entity.adventuretube.Place;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import com.adventuretube.geospatial.kafka.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class AdventureTubeDataFullStackIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdventureTubeDataRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    /** Mock Kafka Producer to avoid requiring a Kafka broker in integration tests */
    @MockitoBean
    private Producer kafkaProducer;

    private final List<String> createdIds = new ArrayList<>();
    private final String testRunId = UUID.randomUUID().toString().substring(0, 8);

    @AfterEach
    void cleanup() {
        if (!createdIds.isEmpty()) {
            repository.deleteAllById(createdIds);
        }
        createdIds.clear();
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private String testYoutubeId(String suffix) {
        return "TEST_YT_" + testRunId + "_" + suffix;
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

    private AdventureTubeData createTestData(String youtubeIdSuffix,
                                              String contentType,
                                              List<String> categories) {
        String ytId = testYoutubeId(youtubeIdSuffix);
        Place place = createPlace("Test Place", 126.978, 37.566);
        Chapter chapter = createChapter(ytId, place);

        return new AdventureTubeData(
                categories,
                List.of(place),
                "3 days",
                "Test description for " + ytId,
                "Test Title " + ytId,
                List.of(chapter),
                contentType,
                "CORE_" + testRunId,
                ytId,
                "strider.test.lee@gmail.com"
        );
    }

    private AdventureTubeData seedAndTrack(String suffix, String contentType, List<String> categories) {
        AdventureTubeData saved = repository.save(createTestData(suffix, contentType, categories));
        createdIds.add(saved.getId());
        return saved;
    }

    // ── GET /geo/data ────────────────────────────────────────────────

    @Test
    void findAll_shouldReturnNonEmptyList() throws Exception {
        seedAndTrack("findAll", "video", List.of("travel"));

        mockMvc.perform(get("/geo/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));
    }

    // ── GET /geo/data/{id} ───────────────────────────────────────────

    @Test
    void findById_shouldReturnData_whenFound() throws Exception {
        AdventureTubeData saved = seedAndTrack("findById", "video", List.of("travel"));

        mockMvc.perform(get("/geo/data/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.youtubeContentID").value(testYoutubeId("findById")));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/geo/data/{id}", "nonexistent_" + testRunId))
                .andExpect(status().isNotFound());
    }

    // ── GET /geo/data/youtube/{youtubeContentID} ─────────────────────

    @Test
    void findByYoutubeContentID_shouldReturnData_whenFound() throws Exception {
        seedAndTrack("ytLookup", "video", List.of("travel"));

        mockMvc.perform(get("/geo/data/youtube/{ytId}", testYoutubeId("ytLookup")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.youtubeContentID").value(testYoutubeId("ytLookup")))
                .andExpect(jsonPath("$.places[0].name").value("Test Place"))
                .andExpect(jsonPath("$.places[0].location.type").value("Point"));
    }

    @Test
    void findByYoutubeContentID_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/geo/data/youtube/{ytId}", "NONEXISTENT_" + testRunId))
                .andExpect(status().isNotFound());
    }

    // ── GET /geo/data/type/{contentType} ─────────────────────────────

    @Test
    void findByContentType_shouldReturnMatchingData() throws Exception {
        String uniqueType = "test_type_" + testRunId;
        seedAndTrack("type1", uniqueType, List.of("travel"));
        seedAndTrack("type2", uniqueType, List.of("food"));

        mockMvc.perform(get("/geo/data/type/{type}", uniqueType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── GET /geo/data/category/{category} ────────────────────────────

    @Test
    void findByCategory_shouldReturnMatchingData() throws Exception {
        String uniqueCat = "test_cat_" + testRunId;
        seedAndTrack("cat1", "video", List.of(uniqueCat, "travel"));
        seedAndTrack("cat2", "video", List.of(uniqueCat, "food"));

        mockMvc.perform(get("/geo/data/category/{cat}", uniqueCat))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── GET /geo/data/count ──────────────────────────────────────────

    @Test
    void count_shouldReturnPositiveNumber() throws Exception {
        seedAndTrack("count", "video", List.of("travel"));

        mockMvc.perform(get("/geo/data/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", greaterThanOrEqualTo(1)));
    }

    // ── POST /geo/save ───────────────────────────────────────────────

    @Test
    void save_shouldReturn202WithTrackingIdAndPublishToKafka() throws Exception {
        AdventureTubeData input = createTestData("postSave", "video", List.of("travel"));

        mockMvc.perform(post("/geo/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.youtubeContentID").value(testYoutubeId("postSave")));

        verify(kafkaProducer).sendAdventureTubeData(any(String.class), any(AdventureTubeData.class));
    }

    // ── POST /geo/save → verify data sent to Producer ─────────────────

    @Test
    void save_shouldSendCorrectDataToProducer() throws Exception {
        AdventureTubeData input = createTestData("roundTrip", "video", List.of("travel", "hiking"));

        mockMvc.perform(post("/geo/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isAccepted());

        org.mockito.ArgumentCaptor<String> trackingIdCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<AdventureTubeData> dataCaptor =
                org.mockito.ArgumentCaptor.forClass(AdventureTubeData.class);
        verify(kafkaProducer).sendAdventureTubeData(trackingIdCaptor.capture(), dataCaptor.capture());

        assertThat(trackingIdCaptor.getValue()).isNotBlank();
        AdventureTubeData captured = dataCaptor.getValue();
        assertThat(captured.getYoutubeContentID()).isEqualTo(testYoutubeId("roundTrip"));
        assertThat(captured.getUserContentType()).isEqualTo("video");
        assertThat(captured.getUserContentCategory()).containsExactly("travel", "hiking");
        assertThat(captured.getPlaces()).hasSize(1);
    }
}
