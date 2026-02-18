package com.adventuretube.web.service.member;


import com.adventuretube.web.model.Webrequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/web")
public class MemberRegisterService {

       @GetMapping(value = "/testsecurity")
       public Mono<ResponseEntity<String>> testSecurity(@RequestParam String storyName , @RequestParam String chapterName){
           return Mono.just(ResponseEntity.ok("Your request "+storyName+" and  "+chapterName+ "is published"));
       }

}
