package com.adventuretube.common.domain.dto.adventuretube;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

// getter, setter, constructor, equal , hash , toString
@Data
@Document

public class Place {
    @Id
    private String id;

    public Place(List<String> contentCategory, long youtubeTime, Location location, String placeID, String name) {
        this.contentCategory = contentCategory;
        this.youtubeTime = youtubeTime;
        this.location = location;
        this.placeID = placeID;
        this.name = name;
    }


    public Place(){
        super();
    }
    private List<String> contentCategory;
    private long youtubeTime;
    private String placeID;
    private String name;
    private Location location;


    @JsonProperty("contentCategory")
    public List<String> getContentCategory() { return contentCategory; }
    @JsonProperty("contentCategory")
    public void setContentCategory(List<String> value) { this.contentCategory = value; }

    @JsonProperty("youtubeTime")
    public long getYoutubeTime() { return youtubeTime; }
    @JsonProperty("youtubeTime")
    public void setYoutubeTime(long value) { this.youtubeTime = value; }

    @JsonProperty("location")
    public Location getLocation() { return location; }
    @JsonProperty("location")
    public void setLocation(Location value) { this.location = value; }

    @JsonProperty("placeID")
    public String getPlaceID() { return placeID; }
    @JsonProperty("placeID")
    public void setPlaceID(String value) { this.placeID = value; }

    @JsonProperty("name")
    public String getName() { return name; }
    @JsonProperty("name")
    public void setName(String value) { this.name = value; }
}