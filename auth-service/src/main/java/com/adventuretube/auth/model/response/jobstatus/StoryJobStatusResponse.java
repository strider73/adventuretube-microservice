package com.adventuretube.auth.model.response.jobstatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StoryJobStatusResponse {
    private String trackingId;
    private String status;
    private String youtubeContentID;
    private int chaptersCount;
    private int placesCount;
    private String errorMessage;
}
