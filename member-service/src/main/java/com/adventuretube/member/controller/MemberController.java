package com.adventuretube.member.controller;

import com.adventuretube.common.domain.dto.UserDTO;
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
    public UserDTO registerMember(@RequestBody UserDTO userDTO){
        log.info("new member registration {}",userDTO);
        return memberService.registerMember(userDTO);
    }


}
