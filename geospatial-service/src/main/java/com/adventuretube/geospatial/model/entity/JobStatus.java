package com.adventuretube.geospatial.model.entity;

import com.adventuretube.geospatial.model.enums.JobStatusEnum;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "jobStatus")
public class JobStatus {
    @Id
    private String id;

    @Indexed(unique = true)
    private String trackingId;

    private String youtubeContentID;
    private JobStatusEnum status;
    private String errorMessage;
    private int chaptersCount;
    private int placesCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Indexed(expireAfter = "7d")
    private LocalDateTime expireAt;
}
