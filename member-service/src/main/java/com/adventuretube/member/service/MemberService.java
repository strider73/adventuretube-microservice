package com.adventuretube.member.service;

import com.adventuretube.member.model.Member;
import com.adventuretube.member.repo.MemberRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@AllArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    public Member registerMember(Member member){
        return  memberRepository.save(member);
    }


    public Optional<Member> findEmail(String email) {
        return memberRepository.findMemberByEmail(email);
    }
}
