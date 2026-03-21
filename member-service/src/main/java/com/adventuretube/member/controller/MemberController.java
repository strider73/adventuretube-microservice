package com.adventuretube.member.controller;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.member.model.dto.member.MemberDTO;
import com.adventuretube.member.model.dto.token.TokenDTO;
import com.adventuretube.member.model.mapper.MemberMapper;
import com.adventuretube.member.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/member")
@AllArgsConstructor
@Slf4j
public class MemberController {
    private final MemberService memberService;
    private final MemberMapper memberMapper;

    @PostMapping("/registerMember")
    public ResponseEntity<ServiceResponse<MemberDTO>> registerMember(@RequestBody MemberDTO memberDTO) {
        log.info("new member registration {}", memberDTO);
        var registeredMember = memberService.registerMember(memberMapper.memberDTOtoMember(memberDTO));
        memberDTO.setId(registeredMember.getId());
        return ResponseEntity.ok(buildResponse("Member registered successfully", memberDTO));
    }

    @PostMapping("/emailDuplicationCheck")
    public ResponseEntity<ServiceResponse<Boolean>> emailDuplicationCheck(@RequestBody String email) {
        boolean exists = memberService.findEmail(email).isPresent();
        String message = exists ? "Email already exists" : "Email is available";
        return ResponseEntity.ok(buildResponse(message, exists));
    }

    @PostMapping("/findMemberByEmail")
    public ResponseEntity<ServiceResponse<MemberDTO>> findMemberByEmail(@RequestBody String email) {
        return memberService.findEmail(email)
                .map(member -> ResponseEntity.ok(buildResponse("Member found", memberMapper.memberToMemberDTO(member))))
                .orElseGet(() -> ResponseEntity.ok(buildResponse("Member not found", (MemberDTO) null)));
    }

    @PostMapping("/storeTokens")
    public ResponseEntity<ServiceResponse<Boolean>> storeToken(@RequestBody TokenDTO tokenDTO) {
        boolean result = memberService.storeToken(tokenDTO);
        return ResponseEntity.ok(buildResponse("Token stored successfully", result));
    }

    @PostMapping("/findToken")
    public ResponseEntity<ServiceResponse<Boolean>> findToken(@RequestBody String token) {
        boolean found = memberService.findToken(token).isPresent();
        String message = found ? "Token found" : "Token not found";
        return ResponseEntity.ok(buildResponse(message, found));
    }

    @PostMapping("/deleteAllToken")
    public ResponseEntity<ServiceResponse<Boolean>> deleteAllToken(@RequestBody String token) {
        boolean deleted = memberService.deleteAllToken(token);
        String message = deleted ? "Token deleted successfully" : "Token not found";
        return ResponseEntity.ok(buildResponse(message, deleted));
    }

    @PostMapping("/deleteUser")
    public ResponseEntity<ServiceResponse<Boolean>> deleteUser(@RequestBody String email) {
        log.info("Deleting user with email: {}", email);
        memberService.deleteUser(email);
        return ResponseEntity.ok(buildResponse("User deleted successfully", true));
    }

    private <T> ServiceResponse<T> buildResponse(String message, T data) {
        return ServiceResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
