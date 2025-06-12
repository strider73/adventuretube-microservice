package com.adventuretube.member.controller;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.member.model.entity.Member;
import com.adventuretube.member.model.dto.member.MemberDTO;
import com.adventuretube.member.model.entity.Token;
import com.adventuretube.member.model.dto.token.TokenDTO;
import com.adventuretube.member.model.mapper.MemberMapper;
import com.adventuretube.member.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final MemberMapper memberMapper;

    @PostMapping("registerMember")
    public ResponseEntity<ServiceResponse<MemberDTO>> registerMember(@RequestBody MemberDTO memberDTO) {
        log.info("new member registration {}", memberDTO);
        Member newMember = memberMapper.memberDTOtoMember(memberDTO);
        try {
            Member registeredMember = memberService.registerMember(newMember);
            memberDTO.setId(registeredMember.getId());

            return ResponseEntity.ok(ServiceResponse.<MemberDTO>builder()
                    .success(true)
                    .message("Member registered successfully")
                    .data(memberDTO)
                    .build());

        } catch (Exception e) {
            log.error("Error occurred while registering member", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ServiceResponse.<MemberDTO>builder()
                            .success(false)
                            .message("Failed to register member")
                            .errorCode("MEMBER_REGISTRATION_FAILED")
                            .data(null)
                            .build());
        }
    }

    @PostMapping("emailDuplicationCheck")
    public ResponseEntity<ServiceResponse<Boolean>> emailDuplicationCheck(@RequestBody String email) {
        Optional<Member> member = memberService.findEmail(email);
        return ResponseEntity.ok(ServiceResponse.<Boolean>builder()
                .success(true)
                .message(member.isPresent() ? "Email already exists" : "Email is available")
                .data(member.isPresent())
                .build());
    }

    @PostMapping("findMemberByEmail")
    public ResponseEntity<ServiceResponse<MemberDTO>> findMemberByEmail(@RequestBody String email) {
        Optional<Member> member = memberService.findEmail(email);
        return ResponseEntity.ok(ServiceResponse.<MemberDTO>builder()
                .success(true)
                .message(member.isPresent() ? "Member found" : "Member not found")
                .data(member.map(memberMapper::memberToMemberDTO).orElse(null))
                .build());
    }

    @PostMapping("storeTokens")
    public ResponseEntity<ServiceResponse<Boolean>> storeToken(@RequestBody TokenDTO tokenDTO) {
        boolean result = memberService.storeToken(tokenDTO);
        return ResponseEntity.ok(ServiceResponse.<Boolean>builder()
                .success(true)
                .message("Token stored successfully")
                .data(result)
                .build());
    }

    @PostMapping("findToken")
    public Boolean findToken(@RequestBody String token) {
        return memberService.findToken(token).isPresent();
    }

    @PostMapping("deleteAllToken")
    public Boolean deleteAllToken(@RequestBody String token) {
        return memberService.deleteAllToken(token);
    }

    @PostMapping("deleteUser")
    public ResponseEntity<ServiceResponse<Boolean>> deleteUser(@RequestBody String email) {
        log.info("Deleting user with email: {}", email);
        try {
            boolean isDeleted = memberService.deleteUser(email);
            if (isDeleted) {
                return ResponseEntity.ok(ServiceResponse.<Boolean>builder()
                        .success(true)
                        .message("User deleted successfully")
                        .data(true)
                        .build());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ServiceResponse.<Boolean>builder()
                        .success(false)
                        .message("User not found")
                        .data(false)
                        .build());
            }
        } catch (Exception e) {
            log.error("Error occurred while deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ServiceResponse.<Boolean>builder()
                    .success(false)
                    .message("Error occurred while deleting user")
                    .errorCode("DELETE_USER_ERROR")
                    .data(null)
                    .build());
        }
    }
}
