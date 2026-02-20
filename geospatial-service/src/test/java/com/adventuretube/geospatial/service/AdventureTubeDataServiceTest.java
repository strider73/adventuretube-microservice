package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.repository.AdventureTubeDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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

        when(repository.findAll()).thenReturn(List.of(data1, data2));

        List<AdventureTubeData> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getYoutubeTitle()).isEqualTo("Trip to Seoul");
        verify(repository).findAll();
    }

    @Test
    void findById_shouldReturnData_whenFound() {
        AdventureTubeData data = new AdventureTubeData();
        data.setId("abc123");
        data.setYoutubeTitle("Mountain Hike");

        when(repository.findById("abc123")).thenReturn(Optional.of(data));

        Optional<AdventureTubeData> result = service.findById("abc123");

        assertThat(result).isPresent();
        assertThat(result.get().getYoutubeTitle()).isEqualTo("Mountain Hike");
        verify(repository).findById("abc123");
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());

        Optional<AdventureTubeData> result = service.findById("nonexistent");

        assertThat(result).isEmpty();
        verify(repository).findById("nonexistent");
    }

    @Test
    void findByYoutubeContentID_shouldReturnData_whenFound() {
        AdventureTubeData data = new AdventureTubeData();
        data.setYoutubeContentID("yt-123");
        data.setYoutubeTitle("Beach Trip");

        when(repository.findByYoutubeContentID("yt-123")).thenReturn(Optional.of(data));

        Optional<AdventureTubeData> result = service.findByYoutubeContentID("yt-123");

        assertThat(result).isPresent();
        assertThat(result.get().getYoutubeContentID()).isEqualTo("yt-123");
        verify(repository).findByYoutubeContentID("yt-123");
    }

    @Test
    void findByYoutubeContentID_shouldReturnEmpty_whenNotFound() {
        when(repository.findByYoutubeContentID("yt-999")).thenReturn(Optional.empty());

        Optional<AdventureTubeData> result = service.findByYoutubeContentID("yt-999");

        assertThat(result).isEmpty();
        verify(repository).findByYoutubeContentID("yt-999");
    }

    @Test
    void findByContentType_shouldReturnMatchingData() {
        AdventureTubeData data = new AdventureTubeData();
        data.setUserContentType("TRAVEL");

        when(repository.findByUserContentType("TRAVEL")).thenReturn(List.of(data));

        List<AdventureTubeData> result = service.findByContentType("TRAVEL");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserContentType()).isEqualTo("TRAVEL");
        verify(repository).findByUserContentType("TRAVEL");
    }

    @Test
    void findByContentType_shouldReturnEmptyList_whenNoneMatch() {
        when(repository.findByUserContentType("UNKNOWN")).thenReturn(List.of());

        List<AdventureTubeData> result = service.findByContentType("UNKNOWN");

        assertThat(result).isEmpty();
        verify(repository).findByUserContentType("UNKNOWN");
    }

    @Test
    void findByCategory_shouldReturnMatchingData() {
        AdventureTubeData data = new AdventureTubeData();
        data.setUserContentCategory(List.of("hiking", "nature"));

        when(repository.findByUserContentCategoryContaining("hiking")).thenReturn(List.of(data));

        List<AdventureTubeData> result = service.findByCategory("hiking");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserContentCategory()).contains("hiking");
        verify(repository).findByUserContentCategoryContaining("hiking");
    }

    @Test
    void findByCategory_shouldReturnEmptyList_whenNoneMatch() {
        when(repository.findByUserContentCategoryContaining("scuba")).thenReturn(List.of());

        List<AdventureTubeData> result = service.findByCategory("scuba");

        assertThat(result).isEmpty();
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

        when(repository.save(input)).thenReturn(saved);

        AdventureTubeData result = service.save(input);

        assertThat(result.getId()).isEqualTo("generated-id");
        assertThat(result.getYoutubeContentID()).isEqualTo("yt-new");
        verify(repository).save(input);
    }

    @Test
    void count_shouldReturnDocumentCount() {
        when(repository.count()).thenReturn(42L);

        long result = service.count();

        assertThat(result).isEqualTo(42L);
        verify(repository).count();
    }
}
