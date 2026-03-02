package com.adventuretube.geospatial.integration;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.model.entity.adventuretube.Chapter;
import com.adventuretube.geospatial.model.entity.adventuretube.Location;
import com.adventuretube.geospatial.model.entity.adventuretube.Place;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.adventuretube.geospatial.kafka.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("integration")
class AdventureTubeDataFullStackIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AdventureTubeDataRepository repository;

    /** Mock Kafka Producer to avoid requiring a Kafka broker in integration tests */
    @MockitoBean
    private Producer kafkaProducer;

    private final List<String> createdIds = new ArrayList<>();
    private final String testRunId = UUID.randomUUID().toString().substring(0, 8);

    @AfterEach
    void cleanup() {
        if (!createdIds.isEmpty()) {
            repository.deleteAllById(createdIds).block();
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
                ytId
        );
    }

    private AdventureTubeData seedAndTrack(String suffix, String contentType, List<String> categories) {
        AdventureTubeData saved = repository.save(createTestData(suffix, contentType, categories)).block();
        createdIds.add(saved.getId());
        return saved;
    }

    // ── GET /geo/data ────────────────────────────────────────────────

    @Test
    void findAll_shouldReturnNonEmptyList() {
        seedAndTrack("findAll", "video", List.of("travel"));

        webTestClient.get().uri("/geo/data")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AdventureTubeData.class)
                .value(list -> assertThat(list).hasSizeGreaterThanOrEqualTo(1));
    }

    // ── GET /geo/data/{id} ───────────────────────────────────────────

    @Test
    void findById_shouldReturnData_whenFound() {
        AdventureTubeData saved = seedAndTrack("findById", "video", List.of("travel"));

        webTestClient.get().uri("/geo/data/{id}", saved.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(saved.getId())
                .jsonPath("$.youtubeContentID").isEqualTo(testYoutubeId("findById"));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() {
        webTestClient.get().uri("/geo/data/{id}", "nonexistent_" + testRunId)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ── GET /geo/data/youtube/{youtubeContentID} ─────────────────────

    @Test
    void findByYoutubeContentID_shouldReturnData_whenFound() {
        seedAndTrack("ytLookup", "video", List.of("travel"));

        webTestClient.get().uri("/geo/data/youtube/{ytId}", testYoutubeId("ytLookup"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.youtubeContentID").isEqualTo(testYoutubeId("ytLookup"))
                .jsonPath("$.places[0].name").isEqualTo("Test Place")
                .jsonPath("$.places[0].location.type").isEqualTo("Point");
    }

    @Test
    void findByYoutubeContentID_shouldReturn404_whenNotFound() {
        webTestClient.get().uri("/geo/data/youtube/{ytId}", "NONEXISTENT_" + testRunId)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ── GET /geo/data/type/{contentType} ─────────────────────────────

    @Test
    void findByContentType_shouldReturnMatchingData() {
        String uniqueType = "test_type_" + testRunId;
        seedAndTrack("type1", uniqueType, List.of("travel"));
        seedAndTrack("type2", uniqueType, List.of("food"));

        webTestClient.get().uri("/geo/data/type/{type}", uniqueType)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AdventureTubeData.class)
                .hasSize(2);
    }

    // ── GET /geo/data/category/{category} ────────────────────────────

    @Test
    void findByCategory_shouldReturnMatchingData() {
        String uniqueCat = "test_cat_" + testRunId;
        seedAndTrack("cat1", "video", List.of(uniqueCat, "travel"));
        seedAndTrack("cat2", "video", List.of(uniqueCat, "food"));

        webTestClient.get().uri("/geo/data/category/{cat}", uniqueCat)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AdventureTubeData.class)
                .hasSize(2);
    }

    // ── GET /geo/data/count ──────────────────────────────────────────

    @Test
    void count_shouldReturnPositiveNumber() {
        seedAndTrack("count", "video", List.of("travel"));

        webTestClient.get().uri("/geo/data/count")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class)
                .value(count -> assertThat(count).isGreaterThanOrEqualTo(1));
    }

    // ── POST /geo/save ───────────────────────────────────────────────

    @Test
    void save_shouldReturn202AndPublishToKafka() {
        AdventureTubeData input = createTestData("postSave", "video", List.of("travel"));

        webTestClient.post().uri("/geo/save")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(input)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains(testYoutubeId("postSave")));

        verify(kafkaProducer).sendAdventureTubeData(any(AdventureTubeData.class));
    }

    // ── POST /geo/save → verify data sent to Producer ─────────────────

    @Test
    void save_shouldSendCorrectDataToProducer() {
        AdventureTubeData input = createTestData("roundTrip", "video", List.of("travel", "hiking"));

        webTestClient.post().uri("/geo/save")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(input)
                .exchange()
                .expectStatus().isAccepted();

        org.mockito.ArgumentCaptor<AdventureTubeData> captor =
                org.mockito.ArgumentCaptor.forClass(AdventureTubeData.class);
        verify(kafkaProducer).sendAdventureTubeData(captor.capture());

        AdventureTubeData captured = captor.getValue();
        assertThat(captured.getYoutubeContentID()).isEqualTo(testYoutubeId("roundTrip"));
        assertThat(captured.getUserContentType()).isEqualTo("video");
        assertThat(captured.getUserContentCategory()).containsExactly("travel", "hiking");
        assertThat(captured.getPlaces()).hasSize(1);
    }
}
