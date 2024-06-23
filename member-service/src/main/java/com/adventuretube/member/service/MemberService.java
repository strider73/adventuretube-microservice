package com.adventuretube.member.service;

import com.adventuretube.common.domain.dto.member.Member;
import com.adventuretube.common.domain.dto.token.TokenDTO;
import com.adventuretube.member.mapper.TokenMapper;
import com.adventuretube.member.repo.MemberRepository;
import com.adventuretube.member.repo.TokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@AllArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final TokenRepository tokenRepository;
    public Member registerMember(Member member){
        return  memberRepository.save(member);
    }


    public Optional<Member> findEmail(String email) {
        return memberRepository.findMemberByEmail(email);
    }

    public Boolean storeToken(TokenDTO tokenDTO){
        //in here tokenDTO.memberDTO will be converted member while TokenDTO transform Token amazing !!!!!!
         tokenRepository.save(TokenMapper.INSTANCE.tokenDTOToToken(tokenDTO));
         return true;
    }
}

