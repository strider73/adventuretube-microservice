package com.adventuretube.service;

import com.adventuretube.model.Member;
import com.adventuretube.repo.MemberRepository;
import com.adventuretube.common.domain.requestmodel.MemberRegistrationRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@AllArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    public void registerMember(MemberRegistrationRequest memberRegistrationRequest){
        Member newMember = Member.builder()
                .name(memberRegistrationRequest.name())
                .email(memberRegistrationRequest.email())
                .channeld(memberRegistrationRequest.channeld())
                .createAt(LocalDateTime.now())
                .build();

        memberRepository.save(newMember);
    }
}
