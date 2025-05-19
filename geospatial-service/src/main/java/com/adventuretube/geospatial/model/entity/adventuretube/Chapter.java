package com.adventuretube.geospatial.model.entity.adventuretube;



import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document
public class Chapter {
    @Id
    private String id;
    private Place place;

    public Chapter(Place place, String youtubeID, long youtubeTime, List<String> categories) {
        this.place = place;
        this.youtubeID = youtubeID;
        this.youtubeTime = youtubeTime;
        this.categories = categories;
    }

    public Chapter(){
        super();
    }

    private String youtubeID;
    private long youtubeTime;
    private List<String> categories;

    @JsonProperty("place")
    public Place getPlace() { return place; }
    @JsonProperty("place")
    public void setPlace(Place value) { this.place = value; }

    @JsonProperty("youtubeId")
    public String getYoutubeID() { return youtubeID; }
    @JsonProperty("youtubeId")
    public void setYoutubeID(String value) { this.youtubeID = value; }

    @JsonProperty("youtubeTime")
    public long getYoutubeTime() { return youtubeTime; }
    @JsonProperty("youtubeTime")
    public void setYoutubeTime(long value) { this.youtubeTime = value; }

    @JsonProperty("categories")
    public List<String> getCategories() { return categories; }
    @JsonProperty("categories")
    public void setCategories(List<String> value) { this.categories = value; }
}