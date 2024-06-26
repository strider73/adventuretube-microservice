package com.adventuretube.geospatial.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/geo")
public class AdventureTubeDataController {

    @GetMapping("/save")
    public ResponseEntity<String> save(){
        return ResponseEntity.ok("Your AdventuretubeData has been saved");
    }

}
