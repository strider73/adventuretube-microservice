package com.adventuretube.geospatial.dto.adventuretube;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.awt.*;

@Document(collection = "locations")
@Getter
@Setter
public class UserLocation {
    @Id
    private String id;


}