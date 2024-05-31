package com.adventuretube.web.service.member;


import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@AllArgsConstructor
@RequestMapping("web/registerMember")
public class MemberRegisterService {
    private final RestTemplate restTemplate;

    @PostMapping
    public void registerMemeber(@RequestBody MemberRegistrationRequest memberRegistrationRequest){
        String url = "http://MEMBER-SERVICE/member/registerMember"; //with Eureka
        System.out.println("http://MEMBER-SERVICE/member/registerMember");
        restTemplate.postForObject(url, memberRegistrationRequest, Void.class);
    }

}
