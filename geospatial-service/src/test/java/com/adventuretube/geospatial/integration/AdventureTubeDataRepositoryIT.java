package com.adventuretube.geospatial.integration;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.model.entity.adventuretube.Chapter;
import com.adventuretube.geospatial.model.entity.adventuretube.Location;
import com.adventuretube.geospatial.model.entity.adventuretube.Place;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ActiveProfiles("integration")
class  AdventureTubeDataRepositoryIT {

    @Autowired
    private AdventureTubeDataRepository repository;

    /** Tracks IDs of documents created during each test for cleanup */
    private final List<String> createdIds = new ArrayList<>();

    /** Unique prefix per test run to avoid collision with real data */
    private final String testRunId = UUID.randomUUID().toString().substring(0, 8);

    @AfterEach
    void cleanup() {
        if (!createdIds.isEmpty()) {
            repository.deleteAllById(createdIds);
        }
        createdIds.clear();
    }

    // ── Helper methods ───────────────────────────────────────────────

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
                "test@example.com"
        );
    }

    /**
     * Save a document and track its ID for cleanup.
     */
    private AdventureTubeData saveAndTrack(AdventureTubeData data) {
        AdventureTubeData saved = repository.save(data);
        createdIds.add(saved.getId());
        return saved;
    }

    // ── Tests ────────────────────────────────────────────────────────

    @Test
    void save_shouldPersistAndGenerateId() {
        AdventureTubeData data = createTestData("save", "video", List.of("travel"));

        AdventureTubeData saved = repository.save(data);
        createdIds.add(saved.getId());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getYoutubeContentID()).isEqualTo(testYoutubeId("save"));
        assertThat(saved.getPlaces()).hasSize(1);
        assertThat(saved.getChapters()).hasSize(1);
    }

    @Test
    void findById_shouldReturnSavedDocument() {
        AdventureTubeData saved = saveAndTrack(
                createTestData("findById", "video", List.of("travel")));

        Optional<AdventureTubeData> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getYoutubeContentID()).isEqualTo(testYoutubeId("findById"));
        assertThat(found.get().getYoutubeTitle()).isEqualTo("Test Title " + testYoutubeId("findById"));
    }

    @Test
    void findByYoutubeContentID_shouldReturnMatchingDocument() {
        AdventureTubeData saved = saveAndTrack(
                createTestData("ytLookup", "video", List.of("travel")));

        Optional<AdventureTubeData> found = repository.findByYoutubeContentID(testYoutubeId("ytLookup"));

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        // Verify nested Place with Location
        assertThat(found.get().getPlaces()).hasSize(1);
        Place place = found.get().getPlaces().get(0);
        assertThat(place.getName()).isEqualTo("Test Place");
        assertThat(place.getLocation().getType()).isEqualTo("Point");
        assertThat(place.getLocation().getCoordinates()).containsExactly(126.978, 37.566);
    }

    @Test
    void findByUserContentType_shouldReturnMatchingDocuments() {
        String uniqueType = "test_type_" + testRunId;
        saveAndTrack(createTestData("type1", uniqueType, List.of("travel")));
        saveAndTrack(createTestData("type2", uniqueType, List.of("food")));

        List<AdventureTubeData> results = repository.findByUserContentType(uniqueType);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(d -> d.getUserContentType().equals(uniqueType));
    }

    @Test
    void findByUserContentCategoryContaining_shouldReturnMatchingDocuments() {
        String uniqueCategory = "test_cat_" + testRunId;
        saveAndTrack(createTestData("cat1", "video", List.of(uniqueCategory, "travel")));
        saveAndTrack(createTestData("cat2", "video", List.of(uniqueCategory, "food")));
        saveAndTrack(createTestData("cat3", "video", List.of("other")));

        List<AdventureTubeData> results = repository.findByUserContentCategoryContaining(uniqueCategory);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(
                d -> d.getUserContentCategory().contains(uniqueCategory));
    }

    @Test
    void findAll_shouldReturnNonEmptyList() {
        saveAndTrack(createTestData("all", "video", List.of("travel")));

        List<AdventureTubeData> all = repository.findAll();
        assertThat(all).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void count_shouldReturnPositiveNumber() {
        saveAndTrack(createTestData("count", "video", List.of("travel")));

        long count = repository.count();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void delete_shouldRemoveDocument() {
        AdventureTubeData saved = saveAndTrack(
                createTestData("delete", "video", List.of("travel")));
        String savedId = saved.getId();

        repository.deleteById(savedId);
        Optional<AdventureTubeData> found = repository.findById(savedId);

        assertThat(found).isEmpty();

        createdIds.remove(savedId); // Already deleted, skip cleanup
    }
}
