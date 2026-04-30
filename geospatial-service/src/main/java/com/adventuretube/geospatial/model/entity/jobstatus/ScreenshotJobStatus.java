package com.adventuretube.geospatial.model.entity.jobstatus;


import com.adventuretube.geospatial.model.enums.ChapterScreenshotJobStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document
public class ScreenshotJobStatus implements JobStatus{
    @Id
    private String id;
    @Indexed(unique = true)
    //The unique index on youtubeContentID prevents duplicate screenshot jobs for the same video
    private String youtubeContentID;
    private String trackingId;
    private ChapterScreenshotJobStatusEnum status;

    private int totalChapters;
    private int completedChapters;

    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Indexed(expireAfter = "7d")
    private LocalDateTime expireAt;


    public boolean isTerminalState() {
        return status != ChapterScreenshotJobStatusEnum.PENDING;
    }

}
