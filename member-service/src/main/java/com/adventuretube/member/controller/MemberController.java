package com.adventuretube.member.controller;

import com.adventuretube.common.domain.dto.auth.MemberDTO;
import com.adventuretube.common.error.RestAPIErrorResponse;
import com.adventuretube.member.model.Member;
import com.adventuretube.member.model.dto.MemberMapper;
import com.adventuretube.member.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("member")
public class MemberController {
    private final MemberService memberService;

    //registerMember's return type for ResponseEntity are  AuthDTO or RestAPIErrorResponse
    //handle carefully on the caller side not by GlobalException handler in member-service
    //since these error should be delivered caller side !!!!
    @PostMapping("registerMember")
    public ResponseEntity<?> registerMember(@RequestBody MemberDTO memberDTO) {
        log.info("new member registration {}", memberDTO);
//        Member newMember = new Member();
//        BeanUtils.copyProperties(memberDTO, newMember);
          Member newMember = MemberMapper.INSTANCE.memberDTOtoMember(memberDTO);
        try {
            //After store in the database nothing but id field will be different
            Member registeredMember = memberService.registerMember(newMember);
            memberDTO.setId(registeredMember.getId());
            return ResponseEntity.ok(memberDTO);
        } catch (Exception e) {
            log.error("Error occurred while registering member", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RestAPIErrorResponse.builder()
                    .message("Error occurred while registering member")
                    .details(e.toString())
                    .statusCode(500)
                    .timestamp(System.currentTimeMillis())
                    .build()
            );
        }

    }


    @PostMapping("emailDuplicationCheck")
    public boolean emailDuplicationCheck(@RequestBody String email) {
        Optional<Member> duplicatedMember = memberService.findEmail(email);
        if (duplicatedMember.isPresent()) {
            return true;
        }
        return false;
    }


    @PostMapping("findMemberByEmail")
    public MemberDTO findMemberByEmail(@RequestBody String email) {
        Optional<Member> member = memberService.findEmail(email);
        if (member.isPresent()) {
            MemberDTO memberDTO = new MemberDTO();
            BeanUtils.copyProperties(member.get(), memberDTO);
            return memberDTO;
        }
        return null;
    }
}


