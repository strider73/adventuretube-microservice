package com.adventuretube.geospatial.controller;

import com.adventuretube.geospatial.GeospatialServiceConfig;
import com.adventuretube.geospatial.kafka.Producer;
import com.adventuretube.geospatial.model.entity.JobStatus;
import com.adventuretube.geospatial.model.entity.adventuretube.AdventureTubeData;
import com.adventuretube.geospatial.model.enums.JobStatusEnum;
import com.adventuretube.geospatial.service.AdventureTubeDataService;
import com.adventuretube.geospatial.service.JobStatusService;
import com.adventuretube.geospatial.sse.SseEmitterManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdventureTubeDataController.class)
@Import(GeospatialServiceConfig.class)
@ActiveProfiles("test")
class AdventureTubeDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdventureTubeDataService adventureTubeDataService;

    @MockitoBean
    private Producer producer;

    @MockitoBean
    private JobStatusService jobStatusService;

    @MockitoBean
    private SseEmitterManager sseEmitterManager;

    // --- GET /geo/data ---

    @Test
    void findAll_shouldReturnListOfData() throws Exception {
        AdventureTubeData data1 = new AdventureTubeData();
        data1.setId("1");
        data1.setYoutubeTitle("Trip to Seoul");

        AdventureTubeData data2 = new AdventureTubeData();
        data2.setId("2");
        data2.setYoutubeTitle("Trip to Tokyo");

        when(adventureTubeDataService.findAll()).thenReturn(List.of(data1, data2));

        mockMvc.perform(get("/geo/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void findAll_shouldReturnEmptyList_whenNoData() throws Exception {
        when(adventureTubeDataService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/geo/data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- GET /geo/data/{id} ---

    @Test
    void findById_shouldReturnData_whenFound() throws Exception {
        AdventureTubeData data = new AdventureTubeData();
        data.setId("abc123");
        data.setYoutubeTitle("Mountain Hike");

        when(adventureTubeDataService.findById("abc123")).thenReturn(Optional.of(data));

        mockMvc.perform(get("/geo/data/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc123"))
                .andExpect(jsonPath("$.youtubeTitle").value("Mountain Hike"));
    }

    @Test
    void findById_shouldReturn404_whenNotFound() throws Exception {
        when(adventureTubeDataService.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/geo/data/nonexistent"))
                .andExpect(status().isNotFound());
    }

    // --- GET /geo/data/youtube/{youtubeContentID} ---

    @Test
    void findByYoutubeContentID_shouldReturnData_whenFound() throws Exception {
        AdventureTubeData data = new AdventureTubeData();
        data.setYoutubeContentID("yt-123");
        data.setYoutubeTitle("Beach Trip");

        when(adventureTubeDataService.findByYoutubeContentID("yt-123")).thenReturn(Optional.of(data));

        mockMvc.perform(get("/geo/data/youtube/yt-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.youtubeContentID").value("yt-123"))
                .andExpect(jsonPath("$.youtubeTitle").value("Beach Trip"));
    }

    @Test
    void findByYoutubeContentID_shouldReturn404_whenNotFound() throws Exception {
        when(adventureTubeDataService.findByYoutubeContentID("yt-999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/geo/data/youtube/yt-999"))
                .andExpect(status().isNotFound());
    }

    // --- GET /geo/data/type/{contentType} ---

    @Test
    void findByContentType_shouldReturnMatchingData() throws Exception {
        AdventureTubeData data = new AdventureTubeData();
        data.setUserContentType("TRAVEL");

        when(adventureTubeDataService.findByContentType("TRAVEL")).thenReturn(List.of(data));

        mockMvc.perform(get("/geo/data/type/TRAVEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findByContentType_shouldReturnEmptyList_whenNoneMatch() throws Exception {
        when(adventureTubeDataService.findByContentType("UNKNOWN")).thenReturn(List.of());

        mockMvc.perform(get("/geo/data/type/UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- GET /geo/data/category/{category} ---

    @Test
    void findByCategory_shouldReturnMatchingData() throws Exception {
        AdventureTubeData data = new AdventureTubeData();
        data.setUserContentCategory(List.of("hiking", "nature"));

        when(adventureTubeDataService.findByCategory("hiking")).thenReturn(List.of(data));

        mockMvc.perform(get("/geo/data/category/hiking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findByCategory_shouldReturnEmptyList_whenNoneMatch() throws Exception {
        when(adventureTubeDataService.findByCategory("scuba")).thenReturn(List.of());

        mockMvc.perform(get("/geo/data/category/scuba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- GET /geo/data/count ---

    @Test
    void count_shouldReturnDocumentCount() throws Exception {
        when(adventureTubeDataService.count()).thenReturn(42L);

        mockMvc.perform(get("/geo/data/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    // --- POST /geo/save ---

    @Test
    void save_shouldReturn202WithTrackingId() throws Exception {
        AdventureTubeData input = new AdventureTubeData();
        input.setYoutubeContentID("yt-new");
        input.setYoutubeTitle("New Adventure");

        JobStatus pendingJob = new JobStatus();
        pendingJob.setTrackingId("test-tracking-id");
        pendingJob.setYoutubeContentID("yt-new");
        pendingJob.setStatus(JobStatusEnum.PENDING);
        pendingJob.setCreatedAt(LocalDateTime.now());
        pendingJob.setUpdatedAt(LocalDateTime.now());

        when(jobStatusService.createPendingJob(anyString(), anyString())).thenReturn(pendingJob);

        mockMvc.perform(post("/geo/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingId").value("test-tracking-id"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(producer).sendAdventureTubeData(anyString(), any(AdventureTubeData.class));
    }

    // --- GET /geo/status/{trackingId} ---

    @Test
    void getStatus_shouldReturnJobStatus_whenFound() throws Exception {
        JobStatus job = new JobStatus();
        job.setTrackingId("abc-123");
        job.setStatus(JobStatusEnum.COMPLETED);
        job.setChaptersCount(3);
        job.setPlacesCount(2);

        when(jobStatusService.findByTrackingId("abc-123")).thenReturn(Optional.of(job));

        mockMvc.perform(get("/geo/status/abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.chaptersCount").value(3));
    }

    @Test
    void getStatus_shouldReturn404_whenNotFound() throws Exception {
        when(jobStatusService.findByTrackingId("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/geo/status/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("JOB_NOT_FOUND"));
    }
}
