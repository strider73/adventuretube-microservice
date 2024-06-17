package com.adventuretube.web.service.member;


import com.adventuretube.web.model.Webrequest;
import lombok.AllArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@AllArgsConstructor
@RequestMapping("/web")
public class MemberRegisterService {
    private final RestTemplate restTemplate;


       @GetMapping(value = "/testsecurity")
       public ResponseEntity<String> testSecurity(@RequestParam String storyName , @RequestParam String chapterName){
           return ResponseEntity.ok("Your request "+storyName+" and  "+chapterName+ "is published");
       }



}
