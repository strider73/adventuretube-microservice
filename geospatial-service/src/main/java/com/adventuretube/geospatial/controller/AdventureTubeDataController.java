package com.adventuretube.geospatial.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/geo")
public class AdventureTubeDataController {

    @GetMapping("/save")
    public Mono<ResponseEntity<String>> save(){
        return Mono.just(ResponseEntity.ok("Your AdventuretubeData has been saved"));
    }

}
