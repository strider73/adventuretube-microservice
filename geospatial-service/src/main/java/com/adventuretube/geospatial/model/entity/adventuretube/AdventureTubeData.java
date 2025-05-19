package com.adventuretube.geospatial.model.entity.adventuretube;


import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document
public class AdventureTubeData {
    @Id
    private String id;
    private List<String> userContentCategory;
    private List<Place> places;
    private String userTripDuration;
    private String youtubeDescription;
    private String youtubeTitle;
    private List<Chapter> chapters;
    private String userContentType;
    private String coreDataID;
    @Indexed(unique = true)
    //because of this index there is property has been added in application.properties
    //spring.data.mongodb.auto-index-creation=true
    private String youtubeContentID;
    public  AdventureTubeData(){
        super();
    }


    public AdventureTubeData(List<String> userContentCategory,
                             List<Place> places,
                             String userTripDuration,
                             String youtubeDescription,
                             String youtubeTitle,
                             List<Chapter> chapters,
                             String userContentType,
                             String coreDataID,
                             String youtubeContentID) {
        this.userContentCategory = userContentCategory;
        this.places = places;
        this.userTripDuration = userTripDuration;
        this.youtubeDescription = youtubeDescription;
        this.youtubeTitle = youtubeTitle;
        this.chapters = chapters;
        this.userContentType = userContentType;
        this.coreDataID = coreDataID;
        this.youtubeContentID = youtubeContentID;
    }

    @JsonProperty("userContentCategory")
    public List<String> getUserContentCategory() { return userContentCategory; }
    @JsonProperty("userContentCategory")
    public void setUserContentCategory(List<String> value) { this.userContentCategory = value; }

    @JsonProperty("places")
    public List<Place> getPlaces() { return places; }
    @JsonProperty("places")
    public void setPlaces(List<Place> value) { this.places = value; }

    @JsonProperty("userTripDuration")
    public String getUserTripDuration() { return userTripDuration; }
    @JsonProperty("userTripDuration")
    public void setUserTripDuration(String value) { this.userTripDuration = value; }

    @JsonProperty("youtubeDescription")
    public String getYoutubeDescription() { return youtubeDescription; }
    @JsonProperty("youtubeDescription")
    public void setYoutubeDescription(String value) { this.youtubeDescription = value; }

    @JsonProperty("youtubeTitle")
    public String getYoutubeTitle() { return youtubeTitle; }
    @JsonProperty("youtubeTitle")
    public void setYoutubeTitle(String value) { this.youtubeTitle = value; }

    @JsonProperty("chapters")
    public List<Chapter> getChapters() { return chapters; }
    @JsonProperty("chapters")
    public void setChapters(List<Chapter> value) { this.chapters = value; }

    @JsonProperty("userContentType")
    public String getUserContentType() { return userContentType; }
    @JsonProperty("userContentType")
    public void setUserContentType(String value) { this.userContentType = value; }

    @JsonProperty("coreDataID")
    public String getCoreDataID() { return coreDataID; }
    @JsonProperty("coreDataID")
    public void setCoreDataID(String value) { this.coreDataID = value; }

    @JsonProperty("youtubeContentID")
    public String getYoutubeContentID() { return youtubeContentID; }
    @JsonProperty("youtubeContentID")
    public void setYoutubeContentID(String value) { this.youtubeContentID = value; }
}