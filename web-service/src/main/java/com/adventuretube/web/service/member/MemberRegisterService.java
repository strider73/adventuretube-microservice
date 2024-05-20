package com.adventuretube.web.service.member;


import com.adventuretube.common.domain.requestmodel.MemberRegistrationRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@AllArgsConstructor
@RequestMapping("api/web/memberRegister")
public class MemberRegisterService {
    private final RestTemplate restTemplate;

    @PostMapping
    public void registerMemeber(@RequestBody MemberRegistrationRequest memberRegistrationRequest){
        //String url = "http://localhost:8070/api/members";
        String url = "http://MEMBER-SERVICE/api/members";
        restTemplate.postForObject(url, memberRegistrationRequest, Void.class);
    }

}
