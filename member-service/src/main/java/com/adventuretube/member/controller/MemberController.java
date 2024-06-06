package com.adventuretube.member.controller;

import com.adventuretube.common.domain.dto.UserDTO;
import com.adventuretube.member.model.Member;
import com.adventuretube.member.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
        Member newMember = Member.builder()
                .username(userDTO.getUsername())
                .password(userDTO.getPassword())
                .email(userDTO.getEmail())
                .channeld(userDTO.getChannelId())
                .role("USER")
                .createAt(LocalDateTime.now())
                .build();
        Member registeredMember = memberService.registerMember(newMember);
        return createUserDTO(registeredMember);
    }


    @PostMapping("emailDuplicationCheck")
    public boolean emailDuplicationCheck(@RequestBody String email){
        Optional<Member>  duplicatedMember = memberService.findEmail(email);
        if(duplicatedMember.isPresent()){
            return true;
        }
        return false;
    }


    @PostMapping("findMemberByEmail")
    public UserDTO  findMemberByEmail(@RequestBody String email){
        Optional<Member> member = memberService.findEmail(email);
        if(member.isPresent()){
            return createUserDTO(member.get());
        }
        return null;
    }



    //when I try to copy member to userDTO using a BeanUtils
    //Id is not copied
    private UserDTO createUserDTO(Member member){
        UserDTO userDTO = new UserDTO();
        userDTO.setId(member.getId());
        userDTO.setPassword(member.getPassword());
        userDTO.setEmail(member.getEmail());
        userDTO.setPassword(member.getPassword());
        userDTO.setRole(member.getRole());
        return userDTO;
    }
}
