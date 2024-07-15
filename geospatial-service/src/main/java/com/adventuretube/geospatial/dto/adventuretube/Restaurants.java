package com.adventuretube.geospatial.dto.adventuretube;

import com.fasterxml.jackson.annotation.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Restaurants {
    @Id
    private String id;
    private Location location;
    private String name;

    @JsonProperty("_id")
    public String getID() { return id; }
    @JsonProperty("_id")
    public void setID(String value) { this.id = value; }

    @JsonProperty("location")
    public Location getLocation() { return location; }
    @JsonProperty("location")
    public void setLocation(Location value) { this.location = value; }

    @JsonProperty("name")
    public String getName() { return name; }
    @JsonProperty("name")
    public void setName(String value) { this.name = value; }
}


