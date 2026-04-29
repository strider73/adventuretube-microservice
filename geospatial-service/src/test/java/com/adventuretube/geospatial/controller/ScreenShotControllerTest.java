package com.adventuretube.geospatial.controller;

import com.adventuretube.geospatial.model.dto.ChapterScreenshotDTO;
import com.adventuretube.geospatial.model.enums.ScreenshotJobStatusEnum;
import com.adventuretube.geospatial.service.ScreenshotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScreenShotController.class)
@ActiveProfiles("test")
class ScreenShotControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private ScreenshotService screenshotService;

    @Test
    void getScreenshotStatus_shouldReturnCompletedWithThumbnails() throws Exception {
        ChapterScreenshotDTO dto = ChapterScreenshotDTO.builder()
                .youtubeContentID("xlumX1Wtzrg")
                .status(ScreenshotJobStatusEnum.COMPLETED)
                .totalChapters(4)
                .completedChapters(4)
                .chapters(List.of(
                        ChapterScreenshotDTO.ChapterScreenshot.builder()
                                .youtubeTime(4).screenshotUrl("xlumX1Wtzrg/ch1_4s.jpg").build(),
                        ChapterScreenshotDTO.ChapterScreenshot.builder()
                                .youtubeTime(397).screenshotUrl("xlumX1Wtzrg/ch2_397s.jpg").build()
                ))
                .build();

        when(screenshotService.getScreenshotWithStatus("xlumX1Wtzrg"))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(get("/geo/screenshot/xlumX1Wtzrg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Screenshot status retrieved"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.chapters.length()").value(2))
                .andExpect(jsonPath("$.data.chapters[0].screenshotUrl").value("xlumX1Wtzrg/ch1_4s.jpg"));
    }

    @Test
    void getScreenshotStatus_shouldReturnPendingWithEmptyThumbnails() throws Exception {
        ChapterScreenshotDTO dto = ChapterScreenshotDTO.builder()
                .youtubeContentID("xlumX1Wtzrg")
                .status(ScreenshotJobStatusEnum.PENDING)
                .totalChapters(4)
                .completedChapters(0)
                .chapters(List.of())
                .build();

        when(screenshotService.getScreenshotWithStatus("xlumX1Wtzrg"))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(get("/geo/screenshot/xlumX1Wtzrg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.chapters.length()").value(0));
    }

    @Test
    void getScreenshotStatus_shouldReturnFailedWithEmptyThumbnails() throws Exception {
        ChapterScreenshotDTO dto = ChapterScreenshotDTO.builder()
                .youtubeContentID("xlumX1Wtzrg")
                .status(ScreenshotJobStatusEnum.FAILED)
                .totalChapters(4)
                .completedChapters(0)
                .errorMessage("yt-dlp failed to download video")
                .chapters(List.of())
                .build();

        when(screenshotService.getScreenshotWithStatus("xlumX1Wtzrg"))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(get("/geo/screenshot/xlumX1Wtzrg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.chapters.length()").value(0))
                .andExpect(jsonPath("$.data.errorMessage").value("yt-dlp failed to download video"));
    }

    @Test
    void getScreenshotStatus_shouldReturnNoJobFound_whenJobDoesNotExist() throws Exception {
        when(screenshotService.getScreenshotWithStatus("unknown-id"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/geo/screenshot/unknown-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("No screenshot job found"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
