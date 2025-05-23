package com.adventuretube.member.controller;


import com.adventuretube.member.model.entity.Member;
import com.adventuretube.member.model.dto.member.MemberDTO;
import com.adventuretube.member.model.entity.Token;
import com.adventuretube.member.model.dto.token.TokenDTO;
import com.adventuretube.member.common.response.RestAPIResponse;
import com.adventuretube.member.model.mapper.MemberMapper;
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
            return ResponseEntity.ok(memberDTO);
        } catch (Exception e) {
            log.error("Error occurred while registering member", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RestAPIResponse.builder()
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

    @PostMapping("storeTokens")
    public Boolean storeToken(@RequestBody TokenDTO tokenDTO){
        //TODO  revoke all token for user
            return memberService.storeToken(tokenDTO);
    }

    @PostMapping("findToken")
    public Boolean findToken(@RequestBody String token){
        Optional<Token> returnedToken = memberService.findToken(token);
        if(returnedToken.isPresent()){
            return true;
        }else{
            return  false;
        }

    }

    @PostMapping("deleteAllToken")
    public Boolean deleteAllToken(@RequestBody String token){
        //TODO  revoke all token for user
         return memberService.deleteAllToken(token);
    }
}




