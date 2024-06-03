package com.adventuretube.web.service.member;


import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@AllArgsConstructor
@RequestMapping("/web")
public class MemberRegisterService {
    private final RestTemplate restTemplate;

//    @PostMapping(value = "/registerMember")
//    public void registerMemeber(@RequestBody AuthRequest request){
//        String url = "http://MEMBER-SERVICE/member/registerMember"; //with Eureka
//        System.out.println("http://MEMBER-SERVICE/member/registerMember");
//        restTemplate.postForObject(url, request, Void.class);
//    }

}
