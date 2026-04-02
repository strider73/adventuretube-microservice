package com.adventuretube.geospatial.model.entity;


import com.adventuretube.geospatial.model.enums.ScreenshotJobStatusEnum;
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
public class ScreenshotJobStatus {
    @Id
    private String id;
    @Indexed(unique = true)
    private String youtubeContentID;
    private ScreenshotJobStatusEnum status;

    private int totalChapters;
    private int completedChapters;

    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Indexed(expireAfter = "7d")
    private LocalDateTime expireAt;


}
