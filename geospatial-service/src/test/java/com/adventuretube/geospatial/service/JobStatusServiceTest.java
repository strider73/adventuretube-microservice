package com.adventuretube.geospatial.service;

import com.adventuretube.geospatial.exceptions.JobNotFoundException;
import com.adventuretube.geospatial.model.entity.JobStatus;
import com.adventuretube.geospatial.model.enums.JobStatusEnum;
import com.adventuretube.geospatial.repository.JobStatusRepository;
import com.adventuretube.geospatial.sse.SseEmitterManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private JobStatusRepository jobStatusRepository;

    @Mock
    private SseEmitterManager sseEmitterManager;

    @InjectMocks
    private JobStatusService jobStatusService;

    @Test
    void createPendingJob_shouldSaveWithCorrectFields() {
        when(jobStatusRepository.save(any(JobStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        JobStatus result = jobStatusService.createPendingJob("track-1", "yt-123");

        assertThat(result.getTrackingId()).isEqualTo("track-1");
        assertThat(result.getYoutubeContentID()).isEqualTo("yt-123");
        assertThat(result.getStatus()).isEqualTo(JobStatusEnum.PENDING);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getExpireAt()).isNotNull();

        verify(jobStatusRepository).save(any(JobStatus.class));
    }

    @Test
    void markCompleted_shouldUpdateStatusAndPushSSE() {
        JobStatus existing = new JobStatus();
        existing.setTrackingId("track-1");
        existing.setStatus(JobStatusEnum.PENDING);

        when(jobStatusRepository.findByTrackingId("track-1")).thenReturn(Optional.of(existing));
        when(jobStatusRepository.save(any(JobStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        JobStatus result = jobStatusService.markCompleted("track-1", 5, 3);

        assertThat(result.getStatus()).isEqualTo(JobStatusEnum.COMPLETED);
        assertThat(result.getChaptersCount()).isEqualTo(5);
        assertThat(result.getPlacesCount()).isEqualTo(3);
        assertThat(result.getErrorMessage()).isNull();

        verify(sseEmitterManager).send(eq("track-1"), any(JobStatus.class));
    }

    @Test
    void markCompletedWithDuplicate_shouldUpdateStatusAndPushSSE() {
        JobStatus existing = new JobStatus();
        existing.setTrackingId("track-2");
        existing.setStatus(JobStatusEnum.PENDING);

        when(jobStatusRepository.findByTrackingId("track-2")).thenReturn(Optional.of(existing));
        when(jobStatusRepository.save(any(JobStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        JobStatus result = jobStatusService.markCompletedWithDuplicate("track-2");

        assertThat(result.getStatus()).isEqualTo(JobStatusEnum.COMPLETED);
        assertThat(result.getErrorMessage()).isEqualTo("DUPLICATE YOUTUBE ID");

        verify(sseEmitterManager).send(eq("track-2"), any(JobStatus.class));
    }

    @Test
    void markFailed_shouldUpdateStatusWithErrorMessage() {
        JobStatus existing = new JobStatus();
        existing.setTrackingId("track-3");
        existing.setStatus(JobStatusEnum.PENDING);

        when(jobStatusRepository.findByTrackingId("track-3")).thenReturn(Optional.of(existing));
        when(jobStatusRepository.save(any(JobStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        JobStatus result = jobStatusService.markFailed("track-3", "Connection refused");

        assertThat(result.getStatus()).isEqualTo(JobStatusEnum.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("Connection refused");

        verify(sseEmitterManager).send(eq("track-3"), any(JobStatus.class));
    }

    @Test
    void markCompleted_shouldThrow_whenTrackingIdNotFound() {
        when(jobStatusRepository.findByTrackingId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobStatusService.markCompleted("unknown", 0, 0))
                .isInstanceOf(JobNotFoundException.class);

        verify(sseEmitterManager, never()).send(anyString(), any());
    }

    @Test
    void findByTrackingId_shouldDelegateToRepository() {
        JobStatus job = new JobStatus();
        job.setTrackingId("track-4");

        when(jobStatusRepository.findByTrackingId("track-4")).thenReturn(Optional.of(job));

        Optional<JobStatus> result = jobStatusService.findByTrackingId("track-4");

        assertThat(result).isPresent();
        assertThat(result.get().getTrackingId()).isEqualTo("track-4");
    }

    @Test
    void findByTrackingId_shouldReturnEmpty_whenNotFound() {
        when(jobStatusRepository.findByTrackingId("unknown")).thenReturn(Optional.empty());

        Optional<JobStatus> result = jobStatusService.findByTrackingId("unknown");

        assertThat(result).isEmpty();
    }
}
