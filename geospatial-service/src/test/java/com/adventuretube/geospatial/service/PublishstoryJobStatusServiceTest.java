package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.exceptions.JobNotFoundException;
import com.adventuretube.geospatial.model.entity.PublishStoryJobStatus;
import com.adventuretube.geospatial.model.enums.PublishStoryJobStatusEnum;
import com.adventuretube.geospatial.repository.PublishStoryJobStatusRepository;
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
class PublishStoryJobStatusServiceTest {

    @Mock
    private PublishStoryJobStatusRepository publishStoryJobStatusRepository;

    @Mock
    private SseEmitterManager sseEmitterManager;

    @InjectMocks
    private PublishStoryJobStatusService publishStoryJobStatusService;

    @Test
    void createPendingJob_shouldSaveWithCorrectFields() {
        when(publishStoryJobStatusRepository.save(any(PublishStoryJobStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        PublishStoryJobStatus result = publishStoryJobStatusService.createPendingJob("track-1", "yt-123");

        assertThat(result.getTrackingId()).isEqualTo("track-1");
        assertThat(result.getYoutubeContentID()).isEqualTo("yt-123");
        assertThat(result.getStatus()).isEqualTo(PublishStoryJobStatusEnum.PENDING);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getExpireAt()).isNotNull();

        verify(publishStoryJobStatusRepository).save(any(PublishStoryJobStatus.class));
    }

    @Test
    void markCompleted_shouldUpdateStatusAndPushSSE() {
        PublishStoryJobStatus existing = new PublishStoryJobStatus();
        existing.setTrackingId("track-1");
        existing.setStatus(PublishStoryJobStatusEnum.PENDING);

        when(publishStoryJobStatusRepository.findByTrackingId("track-1")).thenReturn(Optional.of(existing));
        when(publishStoryJobStatusRepository.save(any(PublishStoryJobStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        PublishStoryJobStatus result = publishStoryJobStatusService.markCompleted("track-1", 5, 3);

        assertThat(result.getStatus()).isEqualTo(PublishStoryJobStatusEnum.COMPLETED);
        assertThat(result.getChaptersCount()).isEqualTo(5);
        assertThat(result.getPlacesCount()).isEqualTo(3);
        assertThat(result.getErrorMessage()).isNull();

        verify(sseEmitterManager).send(eq("track-1"), any(PublishStoryJobStatus.class));
    }

    @Test
    void markCompletedWithDuplicate_shouldUpdateStatusAndPushSSE() {
        PublishStoryJobStatus existing = new PublishStoryJobStatus();
        existing.setTrackingId("track-2");
        existing.setStatus(PublishStoryJobStatusEnum.PENDING);

        when(publishStoryJobStatusRepository.findByTrackingId("track-2")).thenReturn(Optional.of(existing));
        when(publishStoryJobStatusRepository.save(any(PublishStoryJobStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        PublishStoryJobStatus result = publishStoryJobStatusService.markCompletedWithDuplicate("track-2");

        assertThat(result.getStatus()).isEqualTo(PublishStoryJobStatusEnum.DUPLICATED);
        assertThat(result.getErrorMessage()).isEqualTo("DUPLICATE YOUTUBE ID");

        verify(sseEmitterManager).send(eq("track-2"), any(PublishStoryJobStatus.class));
    }

    @Test
    void markFailed_shouldUpdateStatusWithErrorMessage() {
        PublishStoryJobStatus existing = new PublishStoryJobStatus();
        existing.setTrackingId("track-3");
        existing.setStatus(PublishStoryJobStatusEnum.PENDING);

        when(publishStoryJobStatusRepository.findByTrackingId("track-3")).thenReturn(Optional.of(existing));
        when(publishStoryJobStatusRepository.save(any(PublishStoryJobStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        PublishStoryJobStatus result = publishStoryJobStatusService.markFailed("track-3", "Connection refused");

        assertThat(result.getStatus()).isEqualTo(PublishStoryJobStatusEnum.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("Connection refused");

        verify(sseEmitterManager).send(eq("track-3"), any(PublishStoryJobStatus.class));
    }

    @Test
    void markCompleted_shouldThrow_whenTrackingIdNotFound() {
        when(publishStoryJobStatusRepository.findByTrackingId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> publishStoryJobStatusService.markCompleted("unknown", 0, 0))
                .isInstanceOf(JobNotFoundException.class);

        verify(sseEmitterManager, never()).send(anyString(), any());
    }

    @Test
    void findByTrackingId_shouldDelegateToRepository() {
        PublishStoryJobStatus job = new PublishStoryJobStatus();
        job.setTrackingId("track-4");

        when(publishStoryJobStatusRepository.findByTrackingId("track-4")).thenReturn(Optional.of(job));

        Optional<PublishStoryJobStatus> result = publishStoryJobStatusService.findByTrackingId("track-4");

        assertThat(result).isPresent();
        assertThat(result.get().getTrackingId()).isEqualTo("track-4");
    }

    @Test
    void findByTrackingId_shouldReturnEmpty_whenNotFound() {
        when(publishStoryJobStatusRepository.findByTrackingId("unknown")).thenReturn(Optional.empty());

        Optional<PublishStoryJobStatus> result = publishStoryJobStatusService.findByTrackingId("unknown");

        assertThat(result).isEmpty();
    }
}
