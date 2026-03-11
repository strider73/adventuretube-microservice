package com.adventuretube.geospatial.sse;

import com.adventuretube.geospatial.model.entity.JobStatus;
import com.adventuretube.geospatial.model.enums.JobStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SseEmitterManagerTest {

    private SseEmitterManager manager;

    @BeforeEach
    void setUp() {
        manager = new SseEmitterManager();
    }

    @Test
    void register_shouldReturnNonNullSseEmitter() {
        SseEmitter emitter = manager.register("track-1", 30_000L);
        assertThat(emitter).isNotNull();
    }

    @Test
    void register_shouldReturnUniqueEmittersPerTrackingId() {
        SseEmitter emitter1 = manager.register("track-a", 30_000L);
        SseEmitter emitter2 = manager.register("track-b", 30_000L);
        assertThat(emitter1).isNotSameAs(emitter2);
    }

    @Test
    void send_shouldNotThrow_whenNoEmitterRegistered() {
        JobStatus status = new JobStatus();
        status.setTrackingId("unknown");
        status.setStatus(JobStatusEnum.COMPLETED);

        // No emitter registered — should be a silent no-op
        assertThatCode(() -> manager.send("unknown", status)).doesNotThrowAnyException();
    }

    @Test
    void send_shouldBeIdempotent_whenCalledMultipleTimesForUnknownTrackingId() {
        JobStatus status = new JobStatus();
        status.setTrackingId("nonexistent");
        status.setStatus(JobStatusEnum.FAILED);

        assertThatCode(() -> {
            manager.send("nonexistent", status);
            manager.send("nonexistent", status);
        }).doesNotThrowAnyException();
    }
}
