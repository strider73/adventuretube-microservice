package com.adventuretube.member.service;

import com.adventuretube.common.domain.dto.UserDTO;
import com.adventuretube.member.exceptions.DuplicateException;
import com.adventuretube.member.model.Member;
import com.adventuretube.member.repo.MemberRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;


@Service
@AllArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    public UserDTO registerMember(UserDTO userDto){
        Member newMember = Member.builder()
                .username(userDto.getUsername())
                .password(userDto.getPassword())
                .email(userDto.getEmail())
                .channeld(userDto.getChannelId())
                .role("USER")
                .createAt(LocalDateTime.now())
                .build();
//        //TODO need to add password
//        //TODO need to add JWT token

       Member savedMember =  memberRepository.save(newMember);
       UserDTO userDTO = createNormalUserDTO(savedMember);
       return userDTO;
    }

    private UserDTO createNormalUserDTO(Member member){
        UserDTO userDTO = new UserDTO();
          userDTO.setId(member.getId());
          userDTO.setPassword(member.getPassword());
          userDTO.setEmail(member.getEmail());
          userDTO.setPassword(member.getPassword());
          userDTO.setRole(member.getRole());
          return userDTO;
    }

    public Optional<Member> findEmail(String email) {
        return memberRepository.findMemberByEmail(email);
    }
}
