package com.adventuretube.geospatial.sse;

import com.adventuretube.geospatial.model.entity.JobStatus;
import com.adventuretube.geospatial.model.enums.JobStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterManager {

    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String trackingId, long timeoutMs) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        emitters.put(trackingId, emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE completed for trackingId={}", trackingId);
            emitters.remove(trackingId);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE timeout for trackingId={}", trackingId);
            emitters.remove(trackingId);
        });
        emitter.onError(ex -> {
            log.warn("SSE error for trackingId={}: {}", trackingId, ex.getMessage());
            emitters.remove(trackingId);
        });

        return emitter;
    }

    public void send(String trackingId, JobStatus jobStatus) {
        SseEmitter emitter = emitters.get(trackingId);
        if (emitter == null) {
            log.debug("No SSE emitter registered for trackingId={}", trackingId);
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("job-status")
                    .data(jobStatus, MediaType.APPLICATION_JSON));

            // Complete the emitter on terminal states
            if (jobStatus.getStatus() != JobStatusEnum.PENDING) {
                emitter.complete();

                log.info("SSE emitter completed for trackingId={} with status={}", trackingId, jobStatus.getStatus());
            }
        } catch (IOException e) {
            log.warn("Failed to send SSE for trackingId={}: {}", trackingId, e.getMessage());
            emitter.completeWithError(e);
            emitters.remove(trackingId);
        }
    }
}
