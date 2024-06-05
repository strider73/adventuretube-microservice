package com.adventuretube.member.controller;

import com.adventuretube.common.domain.dto.UserDTO;
import com.adventuretube.member.model.Member;
import com.adventuretube.member.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

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


    @PostMapping("emailDuplicationCheck")
    public boolean emailDuplicationCheck(@RequestBody String email){
        Optional<Member>  duplicatedMember = memberService.findEmail(email);
        if(duplicatedMember.isPresent()){
            return true;
        }
        return false;
    }

}
