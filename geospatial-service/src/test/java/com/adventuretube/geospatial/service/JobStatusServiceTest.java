package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.exceptions.JobNotFoundException;
import com.adventuretube.geospatial.model.entity.StoryJobStatus;
import com.adventuretube.geospatial.model.enums.StoryJobStatusEnum;
import com.adventuretube.geospatial.repository.StoryJobStatusRepository;
import com.adventuretube.geospatial.sse.SseEmitterManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobStatusServiceTest {

    @Mock
    private StoryJobStatusRepository storyJobStatusRepository;

    @Mock
    private SseEmitterManager sseEmitterManager;

    @InjectMocks
    private JobStatusService jobStatusService;

    @Test
    void createPendingJob_shouldSaveWithCorrectFields() {
        when(storyJobStatusRepository.save(any(StoryJobStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        StoryJobStatus result = jobStatusService.createPendingJob("track-1", "yt-123");

        assertThat(result.getTrackingId()).isEqualTo("track-1");
        assertThat(result.getYoutubeContentID()).isEqualTo("yt-123");
        assertThat(result.getStatus()).isEqualTo(StoryJobStatusEnum.PENDING);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getExpireAt()).isNotNull();

        verify(storyJobStatusRepository).save(any(StoryJobStatus.class));
    }

    @Test
    void markCompleted_shouldUpdateStatusAndPushSSE() {
        StoryJobStatus existing = new StoryJobStatus();
        existing.setTrackingId("track-1");
        existing.setStatus(StoryJobStatusEnum.PENDING);

        when(storyJobStatusRepository.findByTrackingId("track-1")).thenReturn(Optional.of(existing));
        when(storyJobStatusRepository.save(any(StoryJobStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        StoryJobStatus result = jobStatusService.markCompleted("track-1", 5, 3);

        assertThat(result.getStatus()).isEqualTo(StoryJobStatusEnum.COMPLETED);
        assertThat(result.getChaptersCount()).isEqualTo(5);
        assertThat(result.getPlacesCount()).isEqualTo(3);
        assertThat(result.getErrorMessage()).isNull();

        verify(sseEmitterManager).send(eq("track-1"), any(StoryJobStatus.class));
    }

    @Test
    void markCompletedWithDuplicate_shouldUpdateStatusAndPushSSE() {
        StoryJobStatus existing = new StoryJobStatus();
        existing.setTrackingId("track-2");
        existing.setStatus(StoryJobStatusEnum.PENDING);

        when(storyJobStatusRepository.findByTrackingId("track-2")).thenReturn(Optional.of(existing));
        when(storyJobStatusRepository.save(any(StoryJobStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        StoryJobStatus result = jobStatusService.markCompletedWithDuplicate("track-2");

        assertThat(result.getStatus()).isEqualTo(StoryJobStatusEnum.DUPLICATED);
        assertThat(result.getErrorMessage()).isEqualTo("DUPLICATE YOUTUBE ID");

        verify(sseEmitterManager).send(eq("track-2"), any(StoryJobStatus.class));
    }

    @Test
    void markFailed_shouldUpdateStatusWithErrorMessage() {
        StoryJobStatus existing = new StoryJobStatus();
        existing.setTrackingId("track-3");
        existing.setStatus(StoryJobStatusEnum.PENDING);

        when(storyJobStatusRepository.findByTrackingId("track-3")).thenReturn(Optional.of(existing));
        when(storyJobStatusRepository.save(any(StoryJobStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        StoryJobStatus result = jobStatusService.markFailed("track-3", "Connection refused");

        assertThat(result.getStatus()).isEqualTo(StoryJobStatusEnum.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("Connection refused");

        verify(sseEmitterManager).send(eq("track-3"), any(StoryJobStatus.class));
    }

    @Test
    void markCompleted_shouldThrow_whenTrackingIdNotFound() {
        when(storyJobStatusRepository.findByTrackingId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobStatusService.markCompleted("unknown", 0, 0))
                .isInstanceOf(JobNotFoundException.class);

        verify(sseEmitterManager, never()).send(anyString(), any());
    }

    @Test
    void findByTrackingId_shouldDelegateToRepository() {
        StoryJobStatus job = new StoryJobStatus();
        job.setTrackingId("track-4");

        when(storyJobStatusRepository.findByTrackingId("track-4")).thenReturn(Optional.of(job));

        Optional<StoryJobStatus> result = jobStatusService.findByTrackingId("track-4");

        assertThat(result).isPresent();
        assertThat(result.get().getTrackingId()).isEqualTo("track-4");
    }

    @Test
    void findByTrackingId_shouldReturnEmpty_whenNotFound() {
        when(storyJobStatusRepository.findByTrackingId("unknown")).thenReturn(Optional.empty());

        Optional<StoryJobStatus> result = jobStatusService.findByTrackingId("unknown");

        assertThat(result).isEmpty();
    }
}
