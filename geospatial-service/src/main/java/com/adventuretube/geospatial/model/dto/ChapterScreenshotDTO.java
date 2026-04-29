package com.adventuretube.geospatial.model.dto;

import com.adventuretube.geospatial.model.enums.ScreenshotJobStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterScreenshotDTO {
    private String youtubeContentID;
    private ScreenshotJobStatusEnum status;
    private int totalChapters;
    private int completedChapters;
    private String errorMessage;
    private List<ChapterScreenshot> chapters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterScreenshot {
        private long youtubeTime;
        private String screenshotUrl;
    }
}
