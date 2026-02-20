package com.adventuretube.geospatial.controller;

import com.adventuretube.geospatial.GeospatialServiceConfig;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(AdventureTubeDataController.class)
@Import(GeospatialServiceConfig.class)
@ActiveProfiles("test")
class AdventureTubeDataControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AdventureTubeDataService adventureTubeDataService;

    // --- GET /geo/data ---

    @Test
    void findAll_shouldReturnListOfData() {
        AdventureTubeData data1 = new AdventureTubeData();
        data1.setId("1");
        data1.setYoutubeTitle("Trip to Seoul");

        AdventureTubeData data2 = new AdventureTubeData();
        data2.setId("2");
        data2.setYoutubeTitle("Trip to Tokyo");

        when(adventureTubeDataService.findAll()).thenReturn(List.of(data1, data2));

        webTestClient.get().uri("/geo/data")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AdventureTubeData.class)
                .hasSize(2);
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoData() {
        when(adventureTubeDataService.findAll()).thenReturn(List.of());

        webTestClient.get().uri("/geo/data")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AdventureTubeData.class)
                .hasSize(0);
    }

    // --- GET /geo/data/{id} ---

    @Test
    void findById_shouldReturnData_whenFound() {
        AdventureTubeData data = new AdventureTubeData();
        data.setId("abc123");
        data.setYoutubeTitle("Mountain Hike");

        when(adventureTubeDataService.findById("abc123")).thenReturn(Optional.of(data));

        webTestClient.get().uri("/geo/data/abc123")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("abc123")
                .jsonPath("$.youtubeTitle").isEqualTo("Mountain Hike");
    }

    @Test
    void findById_shouldReturn404_whenNotFound() {
        when(adventureTubeDataService.findById("nonexistent")).thenReturn(Optional.empty());

        webTestClient.get().uri("/geo/data/nonexistent")
                .exchange()
                .expectStatus().isNotFound();
    }

    // --- GET /geo/data/youtube/{youtubeContentID} ---

    @Test
    void findByYoutubeContentID_shouldReturnData_whenFound() {
        AdventureTubeData data = new AdventureTubeData();
        data.setYoutubeContentID("yt-123");
        data.setYoutubeTitle("Beach Trip");

        when(adventureTubeDataService.findByYoutubeContentID("yt-123")).thenReturn(Optional.of(data));

        webTestClient.get().uri("/geo/data/youtube/yt-123")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.youtubeContentID").isEqualTo("yt-123")
                .jsonPath("$.youtubeTitle").isEqualTo("Beach Trip");
    }

    @Test
    void findByYoutubeContentID_shouldReturn404_whenNotFound() {
        when(adventureTubeDataService.findByYoutubeContentID("yt-999")).thenReturn(Optional.empty());

        webTestClient.get().uri("/geo/data/youtube/yt-999")
                .exchange()
                .expectStatus().isNotFound();
    }

    // --- GET /geo/data/type/{contentType} ---

    @Test
    void findByContentType_shouldReturnMatchingData() {
        AdventureTubeData data = new AdventureTubeData();
        data.setUserContentType("TRAVEL");

        when(adventureTubeDataService.findByContentType("TRAVEL")).thenReturn(List.of(data));

        webTestClient.get().uri("/geo/data/type/TRAVEL")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AdventureTubeData.class)
                .hasSize(1);
    }

    @Test
    void findByContentType_shouldReturnEmptyList_whenNoneMatch() {
        when(adventureTubeDataService.findByContentType("UNKNOWN")).thenReturn(List.of());

        webTestClient.get().uri("/geo/data/type/UNKNOWN")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AdventureTubeData.class)
                .hasSize(0);
    }

    // --- GET /geo/data/category/{category} ---

    @Test
    void findByCategory_shouldReturnMatchingData() {
        AdventureTubeData data = new AdventureTubeData();
        data.setUserContentCategory(List.of("hiking", "nature"));

        when(adventureTubeDataService.findByCategory("hiking")).thenReturn(List.of(data));

        webTestClient.get().uri("/geo/data/category/hiking")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AdventureTubeData.class)
                .hasSize(1);
    }

    @Test
    void findByCategory_shouldReturnEmptyList_whenNoneMatch() {
        when(adventureTubeDataService.findByCategory("scuba")).thenReturn(List.of());

        webTestClient.get().uri("/geo/data/category/scuba")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AdventureTubeData.class)
                .hasSize(0);
    }

    // --- GET /geo/data/count ---

    @Test
    void count_shouldReturnDocumentCount() {
        when(adventureTubeDataService.count()).thenReturn(42L);

        webTestClient.get().uri("/geo/data/count")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Long.class)
                .isEqualTo(42L);
    }

    // --- POST /geo/save ---

    @Test
    void save_shouldPersistAndReturnData() {
        AdventureTubeData input = new AdventureTubeData();
        input.setYoutubeContentID("yt-new");
        input.setYoutubeTitle("New Adventure");

        AdventureTubeData saved = new AdventureTubeData();
        saved.setId("generated-id");
        saved.setYoutubeContentID("yt-new");
        saved.setYoutubeTitle("New Adventure");

        when(adventureTubeDataService.save(any(AdventureTubeData.class))).thenReturn(saved);

        webTestClient.post().uri("/geo/save")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(input)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("generated-id")
                .jsonPath("$.youtubeContentID").isEqualTo("yt-new")
                .jsonPath("$.youtubeTitle").isEqualTo("New Adventure");
    }
}
