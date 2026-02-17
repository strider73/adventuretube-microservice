package com.adventuretube.member.controller;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.member.model.dto.member.MemberDTO;
import com.adventuretube.member.model.dto.token.TokenDTO;
import com.adventuretube.member.model.mapper.MemberMapper;
import com.adventuretube.member.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@AllArgsConstructor
@Component
public class MemberHandler {
    private final MemberService memberService;
    private final MemberMapper memberMapper;

    public Mono<ServerResponse> registerMember(ServerRequest request) {
        return request.bodyToMono(MemberDTO.class)
                .flatMap(memberDTO -> {
                    log.info("new member registration {}", memberDTO);
                    return memberService.registerMember(memberMapper.memberDTOtoMember(memberDTO))
                            .map(registeredMember -> {
                                memberDTO.setId(registeredMember.getId());
                                return memberDTO;
                            });
                })
                .flatMap(memberDTO -> buildOkResponse("Member registered successfully", memberDTO));
    }

    public Mono<ServerResponse> emailDuplicationCheck(ServerRequest request) {
        return request.bodyToMono(String.class)
                .flatMap(email -> memberService.findEmail(email)
                        .flatMap(member -> buildOkResponse("Email already exists", true))
                        .switchIfEmpty(buildOkResponse("Email is available", false)));
    }

    public Mono<ServerResponse> findMemberByEmail(ServerRequest request) {
        return request.bodyToMono(String.class)
                .flatMap(email -> memberService.findEmail(email)
                        .flatMap(member -> buildOkResponse("Member found", memberMapper.memberToMemberDTO(member)))
                        .switchIfEmpty(buildOkResponse("Member not found", (MemberDTO) null)));
    }

    public Mono<ServerResponse> storeToken(ServerRequest request) {
        return request.bodyToMono(TokenDTO.class)
                .flatMap(tokenDTO -> memberService.storeToken(tokenDTO))
                .flatMap(result -> buildOkResponse("Token stored successfully", result));
    }

    public Mono<ServerResponse> findToken(ServerRequest request) {
        return request.bodyToMono(String.class)
                .flatMap(token -> memberService.findToken(token)
                        .flatMap(t -> buildOkResponse("Token found", true))
                        .switchIfEmpty(buildOkResponse("Token not found", false)));
    }

    public Mono<ServerResponse> deleteAllToken(ServerRequest request) {
        return request.bodyToMono(String.class)
                .flatMap(token -> memberService.deleteAllToken(token))
                .flatMap(deleted -> {
                    HttpStatus status = deleted ? HttpStatus.OK : HttpStatus.NOT_FOUND;
                    String message = deleted ? "Token deleted successfully" : "Token not found";
                    return buildResponse(status, deleted, message, deleted);
                });
    }

    public Mono<ServerResponse> deleteUser(ServerRequest request) {
        return request.bodyToMono(String.class)
                .flatMap(email -> {
                    log.info("Deleting user with email: {}", email);
                    return memberService.deleteUser(email);
                })
                .flatMap(result -> buildOkResponse("User deleted successfully", true));
    }

    private <T> Mono<ServerResponse> buildOkResponse(String message, T data) {
        return ServerResponse.ok().bodyValue(
                ServiceResponse.<T>builder()
                        .success(true)
                        .message(message)
                        .data(data)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    private <T> Mono<ServerResponse> buildResponse(HttpStatus status, boolean success, String message, T data) {
        return ServerResponse.status(status).bodyValue(
                ServiceResponse.<T>builder()
                        .success(success)
                        .message(message)
                        .data(data)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
