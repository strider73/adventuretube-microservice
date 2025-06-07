package com.adventuretube.member.controller;


import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.member.model.entity.Member;
import com.adventuretube.member.model.dto.member.MemberDTO;
import com.adventuretube.member.model.entity.Token;
import com.adventuretube.member.model.dto.token.TokenDTO;
import com.adventuretube.member.common.response.RestAPIResponse;
import com.adventuretube.member.model.mapper.MemberMapper;
import com.adventuretube.member.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.SecretWithEncapsulation;
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
    private final MemberMapper memberMapper;

    //registerMember's return type for ResponseEntity are  AuthDTO or RestAPIErrorResponse
    //handle carefully on the caller side not by GlobalException handler in member-service
    //since these error should be delivered caller side !!!!
    @PostMapping("registerMember")
    public ResponseEntity<?> registerMember(@RequestBody MemberDTO memberDTO) {
        log.info("new member registration {}", memberDTO);

        /* There is another way to send and receive two or even more different type object using a Map
        1) sender side :
           // Create a map to hold the two objects
                  Map<String, Object> requestMap = new HashMap<>();
                  requestMap.put("memberDTO", registeredUser);
                  requestMap.put("tokenDTO", tokenToStore);
         2) receiver side
             // Extract objects from the map
            ObjectMapper objectMapper = new ObjectMapper();
            MemberDTO memberDTO = objectMapper.convertValue(requestMap.get("memberDTO"), MemberDTO.class);
            TokenDTO tokenDTO = objectMapper.convertValue(requestMap.get("tokenDTO"), TokenDTO.class);

         */
        Member newMember = memberMapper.memberDTOtoMember(memberDTO);
        try {
            //After store in the database nothing but id field will be different
            Member registeredMember = memberService.registerMember(newMember);
            memberDTO.setId(registeredMember.getId());
            return ResponseEntity.ok(
                    ServiceResponse.<MemberDTO>builder()
                            .success(true)
                            .message("Member registered successfully")
                            .data(memberDTO)
                            .build()
            );

        } catch (Exception e) {
            log.error("Error occurred while registering member", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ServiceResponse.<MemberDTO>builder()
                            .success(false)
                            .message("Failed to register member")
                            .errorCode("MEMBER_REGISTRATION_FAILED")
                            .data(null)
                            .build()
            );
        }

    }


    @PostMapping("emailDuplicationCheck")
    public ResponseEntity<ServiceResponse<Boolean>> emailDuplicationCheck(@RequestBody String email) {
        Optional<Member> member = memberService.findEmail(email);
        ServiceResponse<Boolean> response = ServiceResponse.<Boolean>builder()
                .success(true)
                .message(member.isPresent() ? "Email already exists" : "Email is available")
                .data(member.isPresent())
                .build();
        return ResponseEntity.ok(response);
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

    @PostMapping("storeTokens")
    public ResponseEntity<ServiceResponse<Boolean>>  storeToken(@RequestBody TokenDTO tokenDTO) {
        boolean result = memberService.storeToken(tokenDTO);
         ServiceResponse<Boolean> response = ServiceResponse.<Boolean>builder()
                .success(true)
                .message("Token stored successfully")
                .data(result)
                .build();
         return ResponseEntity.ok(response);
    }

    @PostMapping("findToken")
    public Boolean findToken(@RequestBody String token) {
        Optional<Token> returnedToken = memberService.findToken(token);
        if (returnedToken.isPresent()) {
            return true;
        } else {
            return false;
        }

    }

    @PostMapping("deleteAllToken")
    public Boolean deleteAllToken(@RequestBody String token) {
        //TODO  revoke all token for user
        return memberService.deleteAllToken(token);
    }

    @PostMapping("deleteUser")
    public ResponseEntity<?> deleteUser(@RequestBody String email) {
        log.info("Deleting user with email: {}", email);
        try {
            boolean isDeleted = memberService.deleteUser(email);
            if (isDeleted) {
                return ResponseEntity.ok(RestAPIResponse.builder()
                        .message("User deleted successfully")
                        .statusCode(200)
                        .timestamp(System.currentTimeMillis())
                        .build());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(RestAPIResponse.builder()
                        .message("User not found")
                        .statusCode(404)
                        .timestamp(System.currentTimeMillis())
                        .build());
            }
        } catch (Exception e) {
            log.error("Error occurred while deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RestAPIResponse.builder()
                    .message("Error occurred while deleting user")
                    .details(e.toString())
                    .statusCode(500)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }
}




