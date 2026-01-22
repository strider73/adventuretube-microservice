package com.adventuretube.auth.support;

import com.adventuretube.auth.model.dto.member.MemberDTO;
import com.adventuretube.auth.model.mapper.MemberMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("unit")
public class MemberMapperSpringTest {

    private final MemberMapper memberMapper = Mappers.getMapper(MemberMapper.class);

    @Test
    void testUserDetailToMemberDTO() {
        // GIVEN: a mock UserDetails
        UserDetails userDetails = new UserDetails() {
            @Override public String getUsername() { return "testuser@example.com"; }
            @Override public String getPassword() { return "securePassword"; }
            @Override public Collection<? extends GrantedAuthority> getAuthorities() {
                return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            }
            @Override public boolean isAccountNonExpired() { return true; }
            @Override public boolean isAccountNonLocked() { return true; }
            @Override public boolean isCredentialsNonExpired() { return true; }
            @Override public boolean isEnabled() { return true; }
        };

        MemberDTO memberDTO = memberMapper.userDetailToMemberDTO(userDetails);

        assertNotNull(memberDTO);
        assertEquals(memberDTO.getUsername(), userDetails.getUsername());
        assertEquals(memberDTO.getPassword(),userDetails.getPassword());



    }
}
