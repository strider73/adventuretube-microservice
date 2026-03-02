package com.adventuretube.web.service.member;


import com.adventuretube.web.model.Webrequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web")
public class MemberRegisterService {

       @GetMapping(value = "/testsecurity")
       public ResponseEntity<String> testSecurity(@RequestParam String storyName , @RequestParam String chapterName){
           return ResponseEntity.ok("Your request "+storyName+" and  "+chapterName+ "is published");
       }

}
