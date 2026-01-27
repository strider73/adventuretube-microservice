package com.adventuretube.member.controller;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.member.model.entity.Member;
import com.adventuretube.member.model.dto.member.MemberDTO;
import com.adventuretube.member.model.dto.token.TokenDTO;
import com.adventuretube.member.model.mapper.MemberMapper;
import com.adventuretube.member.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("member")
public class MemberController {
    private final MemberService memberService;
    private final MemberMapper memberMapper;

    @PostMapping("registerMember")
    public Mono<ResponseEntity<ServiceResponse<MemberDTO>>> registerMember(@RequestBody MemberDTO memberDTO) {
        log.info("new member registration {}", memberDTO);
        Member newMember = memberMapper.memberDTOtoMember(memberDTO);
        return memberService.registerMember(newMember)
                .map(registeredMember -> {
                    memberDTO.setId(registeredMember.getId());
                    return ResponseEntity.ok(ServiceResponse.<MemberDTO>builder()
                            .success(true)
                            .message("Member registered successfully")
                            .data(memberDTO)
                            .timestamp(LocalDateTime.now())
                            .build());
                });
    }

    @PostMapping("emailDuplicationCheck")
    public Mono<ResponseEntity<ServiceResponse<Boolean>>> emailDuplicationCheck(@RequestBody String email) {
        return memberService.findEmail(email)
                .map(member -> ResponseEntity.ok(ServiceResponse.<Boolean>builder()
                        .success(true)
                        .message("Email already exists")
                        .data(true)
                        .timestamp(LocalDateTime.now())
                        .build()))
                .defaultIfEmpty(ResponseEntity.ok(ServiceResponse.<Boolean>builder()
                        .success(true)
                        .message("Email is available")
                        .data(false)
                        .timestamp(LocalDateTime.now())
                        .build()));
    }

    @PostMapping("findMemberByEmail")
    public Mono<ResponseEntity<ServiceResponse<MemberDTO>>> findMemberByEmail(@RequestBody String email) {
        return memberService.findEmail(email)
                .map(member -> ResponseEntity.ok(ServiceResponse.<MemberDTO>builder()
                        .success(true)
                        .message("Member found")
                        .data(memberMapper.memberToMemberDTO(member))
                        .timestamp(LocalDateTime.now())
                        .build()))
                .defaultIfEmpty(ResponseEntity.ok(ServiceResponse.<MemberDTO>builder()
                        .success(true)
                        .message("Member not found")
                        .data(null)
                        .timestamp(LocalDateTime.now())
                        .build()));
    }

    @PostMapping("storeTokens")
    public Mono<ResponseEntity<ServiceResponse<Boolean>>> storeToken(@RequestBody TokenDTO tokenDTO) {
        return memberService.storeToken(tokenDTO)
                .map(result -> ResponseEntity.ok(ServiceResponse.<Boolean>builder()
                        .success(true)
                        .message("Token stored successfully")
                        .data(result)
                        .timestamp(LocalDateTime.now())
                        .build()));
    }

    @PostMapping("findToken")
    public Mono<ResponseEntity<ServiceResponse<Boolean>>> findToken(@RequestBody String token) {
        return memberService.findToken(token)
                .map(t -> ResponseEntity.ok(ServiceResponse.<Boolean>builder()
                        .success(true)
                        .message("Token found")
                        .data(true)
                        .timestamp(LocalDateTime.now())
                        .build()))
                .defaultIfEmpty(ResponseEntity.ok(ServiceResponse.<Boolean>builder()
                        .success(true)
                        .message("Token not found")
                        .data(false)
                        .timestamp(LocalDateTime.now())
                        .build()));
    }

    @PostMapping("deleteAllToken")
    public Mono<ResponseEntity<ServiceResponse<Boolean>>> deleteAllToken(@RequestBody String token) {
        return memberService.deleteAllToken(token)
                .map(deleted -> {
                    HttpStatus status = deleted ? HttpStatus.OK : HttpStatus.NOT_FOUND;
                    String message = deleted ? "Token deleted successfully" : "Token not found";
                    return ResponseEntity.status(status)
                            .body(ServiceResponse.<Boolean>builder()
                                    .success(deleted)
                                    .message(message)
                                    .data(deleted)
                                    .timestamp(LocalDateTime.now())
                                    .build());
                });
    }

    @PostMapping("deleteUser")
    public Mono<ResponseEntity<ServiceResponse<Boolean>>> deleteUser(@RequestBody String email) {
        log.info("Deleting user with email: {}", email);
        return memberService.deleteUser(email)
                .map(result -> ResponseEntity.ok(ServiceResponse.<Boolean>builder()
                        .success(true)
                        .message("User deleted successfully")
                        .data(true)
                        .timestamp(LocalDateTime.now())
                        .build()));
    }
}
