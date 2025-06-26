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
        Member registeredMember = memberService.registerMember(newMember);
        memberDTO.setId(registeredMember.getId());

        return ResponseEntity.ok(ServiceResponse.<MemberDTO>builder()
                .success(true)
                .message("Member registered successfully")
                .data(memberDTO)
                .timestamp(java.time.LocalDateTime.now())
                .build());
    }

    @PostMapping("emailDuplicationCheck")
    public ResponseEntity<ServiceResponse<Boolean>> emailDuplicationCheck(@RequestBody String email) {
        Optional<Member> member = memberService.findEmail(email);
        return ResponseEntity.ok(ServiceResponse.<Boolean>builder()
                .success(true)
                .message(member.isPresent() ? "Email already exists" : "Email is available")
                .data(member.isPresent())
                .timestamp(java.time.LocalDateTime.now())
                .build());
    }

    @PostMapping("findMemberByEmail")
    public ResponseEntity<ServiceResponse<MemberDTO>> findMemberByEmail(@RequestBody String email) {
        Optional<Member> member = memberService.findEmail(email);
        return ResponseEntity.ok(ServiceResponse.<MemberDTO>builder()
                .success(true)
                .message(member.isPresent() ? "Member found" : "Member not found")
                .data(member.map(memberMapper::memberToMemberDTO).orElse(null))
                .timestamp(java.time.LocalDateTime.now())
                .build());
    }

    @PostMapping("storeTokens")
    public ResponseEntity<ServiceResponse<Boolean>> storeToken(@RequestBody TokenDTO tokenDTO) {
        boolean result = memberService.storeToken(tokenDTO);
        return ResponseEntity.ok(ServiceResponse.<Boolean>builder()
                .success(true)
                .message("Token stored successfully")
                .data(result)
                .timestamp(java.time.LocalDateTime.now())
                .build());
    }

    @PostMapping("findToken")
    public ResponseEntity<ServiceResponse<Boolean>> findToken(@RequestBody String token) {
        boolean exists = memberService.findToken(token).isPresent();
        return ResponseEntity.ok(ServiceResponse.<Boolean>builder()
                .success(true)
                .message(exists ? "Token found" : "Token not found")
                .data(exists)
                .timestamp(java.time.LocalDateTime.now())
                .build());
    }

    @PostMapping("deleteAllToken")
    public ResponseEntity<ServiceResponse<Boolean>> deleteAllToken(@RequestBody String token) {
        boolean deleted = memberService.deleteAllToken(token);
        HttpStatus status = deleted ? HttpStatus.OK : HttpStatus.NOT_FOUND;
        String message = deleted ? "Token deleted successfully" : "Token not found";
        return ResponseEntity.status(status)
                .body(ServiceResponse.<Boolean>builder()
                        .success(deleted)
                        .message(message)
                        .data(deleted)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    @PostMapping("deleteUser")
    public ResponseEntity<ServiceResponse<Boolean>> deleteUser(@RequestBody String email) {
        log.info("Deleting user with email: {}", email);
        memberService.deleteUser(email); // Exception flows to GlobalExceptionHandler
        return ResponseEntity.ok(ServiceResponse.<Boolean>builder()
                .success(true)
                .message("User deleted successfully")
                .data(true)
                .timestamp(java.time.LocalDateTime.now())
                .build());
    }
}
