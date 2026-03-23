package com.adventuretube.member.controller;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.member.model.dto.member.MemberDTO;
import com.adventuretube.member.model.dto.token.TokenDTO;
import com.adventuretube.member.model.mapper.MemberMapper;
import com.adventuretube.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Member Controller", description = "Internal member CRUD and token management endpoints. Called by auth-service, not exposed to external clients.")
public class MemberController {
    private final MemberService memberService;
    private final MemberMapper memberMapper;

    @Operation(summary = "Register a new member")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Member registered successfully."),
            @ApiResponse(responseCode = "409", description = "Email already exists.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "User already exists with the provided email",
                                      "errorCode": "USER_EMAIL_DUPLICATE",
                                      "data": "member-service",
                                      "timestamp": "2026-03-23T14:00:00"
                                    }
                                    """))),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Unknown error",
                                      "errorCode": "UNKNOWN_EXCEPTION",
                                      "data": "member-service",
                                      "timestamp": "2026-03-23T14:00:00"
                                    }
                                    """)))
    })
    @PostMapping("/registerMember")
    public ResponseEntity<ServiceResponse<MemberDTO>> registerMember(@RequestBody MemberDTO memberDTO) {
        log.info("new member registration {}", memberDTO);
        var registeredMember = memberService.registerMember(memberMapper.memberDTOtoMember(memberDTO));
        memberDTO.setId(registeredMember.getId());
        return ResponseEntity.ok(buildResponse("Member registered successfully", memberDTO));
    }

    @Operation(summary = "Check if email already exists")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email check completed.")
    })
    @PostMapping("/emailDuplicationCheck")
    public ResponseEntity<ServiceResponse<Boolean>> emailDuplicationCheck(@RequestBody String email) {
        boolean exists = memberService.findEmail(email).isPresent();
        String message = exists ? "Email already exists" : "Email is available";
        return ResponseEntity.ok(buildResponse(message, exists));
    }

    @Operation(summary = "Find member by email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Member lookup completed. Data is null if not found.")
    })
    @PostMapping("/findMemberByEmail")
    public ResponseEntity<ServiceResponse<MemberDTO>> findMemberByEmail(@RequestBody String email) {
        return memberService.findEmail(email)
                .map(member -> ResponseEntity.ok(buildResponse("Member found", memberMapper.memberToMemberDTO(member))))
                .orElseGet(() -> ResponseEntity.ok(buildResponse("Member not found", (MemberDTO) null)));
    }

    @Operation(summary = "Store access and refresh tokens for a member")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token stored successfully.")
    })
    @PostMapping("/storeTokens")
    public ResponseEntity<ServiceResponse<Boolean>> storeToken(@RequestBody TokenDTO tokenDTO) {
        boolean result = memberService.storeToken(tokenDTO);
        return ResponseEntity.ok(buildResponse("Token stored successfully", result));
    }

    @Operation(summary = "Check if a token exists in the database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token lookup completed.")
    })
    @PostMapping("/findToken")
    public ResponseEntity<ServiceResponse<Boolean>> findToken(@RequestBody String token) {
        boolean found = memberService.findToken(token).isPresent();
        String message = found ? "Token found" : "Token not found";
        return ResponseEntity.ok(buildResponse(message, found));
    }

    @Operation(summary = "Delete all tokens associated with a refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token deletion completed.")
    })
    @PostMapping("/deleteAllToken")
    public ResponseEntity<ServiceResponse<Boolean>> deleteAllToken(@RequestBody String token) {
        boolean deleted = memberService.deleteAllToken(token);
        String message = deleted ? "Token deleted successfully" : "Token not found";
        return ResponseEntity.ok(buildResponse(message, deleted));
    }

    @Operation(summary = "Delete a user and all associated tokens by email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully."),
            @ApiResponse(responseCode = "404", description = "User not found.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "User not found",
                                      "errorCode": "USER_NOT_FOUND",
                                      "data": "member-service",
                                      "timestamp": "2026-03-23T14:00:00"
                                    }
                                    """)))
    })
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
