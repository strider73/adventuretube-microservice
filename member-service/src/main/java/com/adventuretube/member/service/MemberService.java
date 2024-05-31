package com.adventuretube.member.service;

import com.adventuretube.common.domain.dto.UserDTO;
import com.adventuretube.common.domain.requestmodel.AuthRequest;
import com.adventuretube.member.model.Member;
import com.adventuretube.member.repo.MemberRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@AllArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    public UserDTO registerMember(AuthRequest request){
        Member newMember = Member.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .email(request.getEmail())
                .channeld(request.getChannelID())
                .role("USER")
                .createAt(LocalDateTime.now())
                .build();
        //TODO need to add password
        //TODO need to add JWT token
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
}
