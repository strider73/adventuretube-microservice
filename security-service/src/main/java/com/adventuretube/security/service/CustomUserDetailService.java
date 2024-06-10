package com.adventuretube.security.service;

import com.adventuretube.common.domain.dto.auth.AuthDTO;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private  RestTemplate restTemplate;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String urlForFindUserByEmail = "http://MEMBER-SERVICE/member/findMemberByEmail";

        // Fetch user details from the external service
        AuthDTO userFoundByEmail;
        try {
            userFoundByEmail = restTemplate.postForObject(urlForFindUserByEmail, email, AuthDTO.class);
        } catch (Exception e) {
            throw new UsernameNotFoundException("User not found with email: " + email, e);
        }

        if (userFoundByEmail == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        // Build UserDetails object
        return User.builder()
                .username(userFoundByEmail.getEmail())
                .password(userFoundByEmail.getPassword())
                .authorities(userFoundByEmail.getRole()) // Ensure roles are in the correct format
                .build();
    }
}
