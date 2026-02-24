package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdventureTubeDataServiceTest {

    @Mock
    private AdventureTubeDataRepository repository;

    @InjectMocks
    private AdventureTubeDataService service;

    @Test
    void findAll_shouldReturnAllData() {
        AdventureTubeData data1 = new AdventureTubeData();
        data1.setId("1");
        data1.setYoutubeTitle("Trip to Seoul");

        AdventureTubeData data2 = new AdventureTubeData();
        data2.setId("2");
        data2.setYoutubeTitle("Trip to Tokyo");

        when(repository.findAll()).thenReturn(Flux.just(data1, data2));

        StepVerifier.create(service.findAll())
                .assertNext(d -> assertThat(d.getYoutubeTitle()).isEqualTo("Trip to Seoul"))
                .assertNext(d -> assertThat(d.getYoutubeTitle()).isEqualTo("Trip to Tokyo"))
                .verifyComplete();

        verify(repository).findAll();
    }

    @Test
    void findById_shouldReturnData_whenFound() {
        AdventureTubeData data = new AdventureTubeData();
        data.setId("abc123");
        data.setYoutubeTitle("Mountain Hike");

        when(repository.findById("abc123")).thenReturn(Mono.just(data));

        StepVerifier.create(service.findById("abc123"))
                .assertNext(d -> {
                    assertThat(d.getId()).isEqualTo("abc123");
                    assertThat(d.getYoutubeTitle()).isEqualTo("Mountain Hike");
                })
                .verifyComplete();

        verify(repository).findById("abc123");
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        when(repository.findById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(service.findById("nonexistent"))
                .verifyComplete();

        verify(repository).findById("nonexistent");
    }

    @Test
    void findByYoutubeContentID_shouldReturnData_whenFound() {
        AdventureTubeData data = new AdventureTubeData();
        data.setYoutubeContentID("yt-123");
        data.setYoutubeTitle("Beach Trip");

        when(repository.findByYoutubeContentID("yt-123")).thenReturn(Mono.just(data));

        StepVerifier.create(service.findByYoutubeContentID("yt-123"))
                .assertNext(d -> {
                    assertThat(d.getYoutubeContentID()).isEqualTo("yt-123");
                    assertThat(d.getYoutubeTitle()).isEqualTo("Beach Trip");
                })
                .verifyComplete();

        verify(repository).findByYoutubeContentID("yt-123");
    }

    @Test
    void findByYoutubeContentID_shouldReturnEmpty_whenNotFound() {
        when(repository.findByYoutubeContentID("yt-999")).thenReturn(Mono.empty());

        StepVerifier.create(service.findByYoutubeContentID("yt-999"))
                .verifyComplete();

        verify(repository).findByYoutubeContentID("yt-999");
    }

    @Test
    void findByContentType_shouldReturnMatchingData() {
        AdventureTubeData data = new AdventureTubeData();
        data.setUserContentType("TRAVEL");

        when(repository.findByUserContentType("TRAVEL")).thenReturn(Flux.just(data));

        StepVerifier.create(service.findByContentType("TRAVEL"))
                .assertNext(d -> assertThat(d.getUserContentType()).isEqualTo("TRAVEL"))
                .verifyComplete();

        verify(repository).findByUserContentType("TRAVEL");
    }

    @Test
    void findByContentType_shouldReturnEmptyFlux_whenNoneMatch() {
        when(repository.findByUserContentType("UNKNOWN")).thenReturn(Flux.empty());

        StepVerifier.create(service.findByContentType("UNKNOWN"))
                .verifyComplete();

        verify(repository).findByUserContentType("UNKNOWN");
    }

    @Test
    void findByCategory_shouldReturnMatchingData() {
        AdventureTubeData data = new AdventureTubeData();
        data.setUserContentCategory(List.of("hiking", "nature"));

        when(repository.findByUserContentCategoryContaining("hiking")).thenReturn(Flux.just(data));

        StepVerifier.create(service.findByCategory("hiking"))
                .assertNext(d -> assertThat(d.getUserContentCategory()).contains("hiking"))
                .verifyComplete();

        verify(repository).findByUserContentCategoryContaining("hiking");
    }

    @Test
    void findByCategory_shouldReturnEmptyFlux_whenNoneMatch() {
        when(repository.findByUserContentCategoryContaining("scuba")).thenReturn(Flux.empty());

        StepVerifier.create(service.findByCategory("scuba"))
                .verifyComplete();

        verify(repository).findByUserContentCategoryContaining("scuba");
    }

    @Test
    void save_shouldPersistAndReturnData() {
        AdventureTubeData input = new AdventureTubeData();
        input.setYoutubeContentID("yt-new");
        input.setYoutubeTitle("New Adventure");

        AdventureTubeData saved = new AdventureTubeData();
        saved.setId("generated-id");
        saved.setYoutubeContentID("yt-new");
        saved.setYoutubeTitle("New Adventure");

        when(repository.save(input)).thenReturn(Mono.just(saved));

        StepVerifier.create(service.save(input))
                .assertNext(d -> {
                    assertThat(d.getId()).isEqualTo("generated-id");
                    assertThat(d.getYoutubeContentID()).isEqualTo("yt-new");
                })
                .verifyComplete();

        verify(repository).save(input);
    }

    @Test
    void count_shouldReturnDocumentCount() {
        when(repository.count()).thenReturn(Mono.just(42L));

        StepVerifier.create(service.count())
                .assertNext(count -> assertThat(count).isEqualTo(42L))
                .verifyComplete();

        verify(repository).count();
    }
}
