package com.adventuretube.web.controller;

import com.adventuretube.web.service.ChapterScreenShotService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import com.adventuretube.web.config.WebServiceConfig;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = ChapterScreenShotController.class)
@Import(WebServiceConfig.class)
@ActiveProfiles("test")
class ChapterScreenShotControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChapterScreenShotService chapterScreenShotService;

    @Test
    void getScreenshotStatus_shouldReturnCompletedWithThumbnails() throws Exception {
        String upstreamJson = """
                {
                  "success": true,
                  "message": "Screenshot status retrieved",
                  "data": {
                    "youtubeContentID": "xlumX1Wtzrg",
                    "status": "COMPLETED",
                    "totalChapters": 4,
                    "completedChapters": 4,
                    "chapters": [
                      { "youtubeTime": 4,   "screenshotUrl": "xlumX1Wtzrg/ch1_4s.jpg" },
                      { "youtubeTime": 397, "screenshotUrl": "xlumX1Wtzrg/ch2_397s.jpg" }
                    ]
                  }
                }
                """;
        JsonNode upstream = objectMapper.readTree(upstreamJson);

        when(chapterScreenShotService.getScreenshotStatus("xlumX1Wtzrg"))
                .thenReturn(Mono.just(upstream));

        webTestClient.get().uri("/web/chapter/screenshot/xlumX1Wtzrg")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo("Screenshot status retrieved")
                .jsonPath("$.data.data.status").isEqualTo("COMPLETED")
                .jsonPath("$.data.data.chapters.length()").isEqualTo(2)
                .jsonPath("$.data.data.chapters[0].screenshotUrl").isEqualTo("xlumX1Wtzrg/ch1_4s.jpg");
    }

    @Test
    void getScreenshotStatus_shouldReturnPendingWithEmptyThumbnails() throws Exception {
        String upstreamJson = """
                {
                  "success": true,
                  "message": "Screenshot status retrieved",
                  "data": {
                    "youtubeContentID": "xlumX1Wtzrg",
                    "status": "PENDING",
                    "totalChapters": 4,
                    "completedChapters": 0,
                    "chapters": []
                  }
                }
                """;
        JsonNode upstream = objectMapper.readTree(upstreamJson);

        when(chapterScreenShotService.getScreenshotStatus("xlumX1Wtzrg"))
                .thenReturn(Mono.just(upstream));

        webTestClient.get().uri("/web/chapter/screenshot/xlumX1Wtzrg")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.data.status").isEqualTo("PENDING")
                .jsonPath("$.data.data.chapters.length()").isEqualTo(0);
    }

    @Test
    void getScreenshotStatus_shouldReturnFailedWithErrorMessage() throws Exception {
        String upstreamJson = """
                {
                  "success": true,
                  "message": "Screenshot status retrieved",
                  "data": {
                    "youtubeContentID": "xlumX1Wtzrg",
                    "status": "FAILED",
                    "totalChapters": 4,
                    "completedChapters": 0,
                    "errorMessage": "yt-dlp failed to download video",
                    "chapters": []
                  }
                }
                """;
        JsonNode upstream = objectMapper.readTree(upstreamJson);

        when(chapterScreenShotService.getScreenshotStatus("xlumX1Wtzrg"))
                .thenReturn(Mono.just(upstream));

        webTestClient.get().uri("/web/chapter/screenshot/xlumX1Wtzrg")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.data.status").isEqualTo("FAILED")
                .jsonPath("$.data.data.chapters.length()").isEqualTo(0)
                .jsonPath("$.data.data.errorMessage").isEqualTo("yt-dlp failed to download video");
    }

    @Test
    void getScreenshotStatus_shouldReturnNoJobFound_whenJobDoesNotExist() throws Exception {
        String upstreamJson = """
                {
                  "success": true,
                  "message": "No screenshot job found"
                }
                """;
        JsonNode upstream = objectMapper.readTree(upstreamJson);

        when(chapterScreenShotService.getScreenshotStatus("unknown-id"))
                .thenReturn(Mono.just(upstream));

        webTestClient.get().uri("/web/chapter/screenshot/unknown-id")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.message").isEqualTo("No screenshot job found")
                .jsonPath("$.data.data").doesNotExist();
    }
}
