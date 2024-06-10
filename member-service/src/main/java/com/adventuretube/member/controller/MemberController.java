package com.adventuretube.member.controller;

import com.adventuretube.common.domain.dto.auth.AuthRequestDTO;
import com.adventuretube.common.error.CommonErrorResponse;
import com.adventuretube.member.model.Member;
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

    @PostMapping("registerMember")
    public ResponseEntity<?> registerMember(@RequestBody AuthRequestDTO authRequestDTO) {
        log.info("new member registration {}", authRequestDTO);
        Member newMember = new Member();
        BeanUtils.copyProperties(authRequestDTO, newMember);
        try {
            //After store in the database nothing but id field will be different
            Member registeredMember = memberService.registerMember(newMember);
            authRequestDTO.setId(registeredMember.getId());
            return ResponseEntity.ok(authRequestDTO);
        } catch (Exception e) {
            log.error("Error occurred while registering member", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonErrorResponse.builder()
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
    public AuthRequestDTO findMemberByEmail(@RequestBody String email) {
        Optional<Member> member = memberService.findEmail(email);
        if (member.isPresent()) {
            AuthRequestDTO authRequestDTO = new AuthRequestDTO();
            BeanUtils.copyProperties(member, authRequestDTO);
            return authRequestDTO;
        }
        return null;
    }
}


    //when I try to copy member to userDTO using a BeanUtils
    //Id is not copied
//    private AuthRequestDTO createUserDTO(Member member){
//        AuthRequestDTO userDTO = new AuthRequestDTO();
//        userDTO.setId(member.getId());
//        userDTO.setPassword(member.getPassword());
//        userDTO.setEmail(member.getEmail());
//        userDTO.setPassword(member.getPassword());
//        userDTO.setRole(member.getRole());
//        return userDTO;
//    }
//}
