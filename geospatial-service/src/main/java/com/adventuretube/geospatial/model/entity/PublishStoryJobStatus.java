package com.adventuretube.geospatial.model.entity;

import com.adventuretube.geospatial.model.enums.PublishStoryJobStatusEnum;
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
@Document(collection = "publishStoryJobStatus")
public class PublishStoryJobStatus {
    @Id
    private String id;

    @Indexed(unique = true)
    private String trackingId;

    private String youtubeContentID;
    private PublishStoryJobStatusEnum status;
    private String errorMessage;
    private int chaptersCount;
    private int placesCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Indexed(expireAfter = "7d")
    private LocalDateTime expireAt;
}
