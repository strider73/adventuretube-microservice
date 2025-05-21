package com.adventuretube.member.model.mapper;

import com.adventuretube.member.model.dto.member.MemberDTO;
import com.adventuretube.member.model.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class MemberMapperSpringTest {

    @Autowired
    private MemberMapper memberMapper;

    @Test
    void testMemberDTOtoMember() {
        UUID uuid = UUID.randomUUID();
        MemberDTO dto = MemberDTO.builder()
                .id(uuid)
                .email("user@example.com")
                .password("securePassword")
                .username("testuser")
                .googleIdToken("dummyGoogleIdToken")
                .googleIdTokenExp(1234567890L)
                .googleIdTokenIat(1234567000L)
                .googleIdTokenSub("sub123")
                .googleProfilePicture("https://img.example.com/pic.png")
                .channelId("channelXYZ")
                .role("USER")
                .build();

        Member member = memberMapper.memberDTOtoMember(dto);

        assertNotNull(member);
        assertEquals(dto.getEmail(), member.getEmail());
        assertEquals(dto.getRole(), member.getRole());
    }

    @Test
    void testMemberToMemberDTO() {
        UUID uuid = UUID.randomUUID();
        Member member = new Member();
        member.setId(uuid);
        member.setEmail("user@example.com");
        member.setPassword("securePassword");
        member.setUsername("testuser");
        member.setGoogleIdToken("dummyGoogleIdToken");
        member.setGoogleIdTokenExp(1234567890L);
        member.setGoogleIdTokenIat(1234567000L);
        member.setGoogleIdTokenSub("sub123");
        member.setGoogleProfilePicture("https://img.example.com/pic.png");
        member.setChannelId("channelXYZ");
        member.setRole("USER");

        MemberDTO dto = memberMapper.memberToMemberDTO(member);

        assertNotNull(dto);
        assertEquals(member.getEmail(), dto.getEmail());
        assertEquals(member.getRole(), dto.getRole());
    }
}
