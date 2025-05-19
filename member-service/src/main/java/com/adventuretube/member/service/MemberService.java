package com.adventuretube.member.service;



import com.adventuretube.member.model.dto.token.TokenDTO;
import com.adventuretube.member.model.entity.Member;
import com.adventuretube.member.model.entity.Token;
import com.adventuretube.member.model.mapper.MemberMapper;
import com.adventuretube.member.model.mapper.TokenMapper;
import com.adventuretube.member.repo.MemberRepository;
import com.adventuretube.member.repo.TokenRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
@AllArgsConstructor
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final TokenRepository tokenRepository;
    public Member registerMember(Member member){
        return  memberRepository.save(member);
    }


    public Optional<Member> findEmail(String email) {
        return memberRepository.findMemberByEmail(email);
    }

    public Optional<Member> findMemberByEmailAndTokenCheck(String email) {
        Optional<Member> member = memberRepository.findMemberByEmail(email);
        if(member.isPresent()) {
            List<Token> tokens = tokenRepository.findAllValidTokenByMember(member.get().getId());
            if (tokens.isEmpty()){
                throw new RuntimeException("Member with email "+email+" was logged out already ");
            }else{
                return member;
            }
        }else{
            throw new RuntimeException("Member with email "+email+" is not exist");
        }
    }

    public Boolean storeToken(TokenDTO tokenDTO){
        //in here tokenDTO.memberDTO will be converted member while TokenDTO transform Token amazing !!!!!!
        //before store need to check the tokenDTO.memberDTO.id
        //this will be the case for
        //       1. login process ::  authenticate
        //       2. refresh token process
        if(tokenDTO.memberDTO.getId() == null){
          Optional<Member> member =  memberRepository.findMemberByEmail(tokenDTO.memberDTO.getUsername());
          if(member.isPresent()){
              tokenDTO.setMemberDTO(MemberMapper.INSTANCE.memberToMemberDTO(member.get()));
          }else{
              throw new RuntimeException("User email "+tokenDTO.memberDTO.getEmail() + " is not a Member");
          }
        }
        Token token = TokenMapper.INSTANCE.tokenDTOToToken(tokenDTO);
        List<Token> tokens = tokenRepository.findAllValidTokenByMember(tokenDTO.memberDTO.getId());
        tokens.forEach(tokenRepository::delete);
        tokenRepository.save(TokenMapper.INSTANCE.tokenDTOToToken(tokenDTO));
        return true;
    }

    public Optional<Token> findToken(String token) {
        return  tokenRepository.findByRefreshToken(token);
    }

    public Boolean deleteAllToken(String token) {
       int deleteCount =  tokenRepository.deleteAllTokenByAccessToken(token);
       if(deleteCount > 0) {
           log.debug("token delete successfully");
           return true;
       }else{
           log.warn("Token deletion failed for access token: {}", token);
           return false;
       }
    }
}

