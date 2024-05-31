package com.adventuretube.member.controller;

import com.adventuretube.common.domain.requestmodel.MemberRegistrationRequest;
import com.adventuretube.member.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("member")
public class MemberController {
    private final MemberService memberService;
    @PostMapping("registerMember")
    public void registerMember(@RequestBody MemberRegistrationRequest memberRegistrationRequest){
        log.info("new member registration {}",memberRegistrationRequest);
        memberService.registerMember(memberRegistrationRequest);
    }


}
