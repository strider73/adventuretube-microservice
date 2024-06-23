package com.adventuretube.auth.service;

import com.adventuretube.auth.exceptions.UserNotFoundException;
import com.adventuretube.common.domain.dto.member.MemberDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@AllArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private  RestTemplate restTemplate;

    //UsernameNotFoundException will be handled by Spring Security resulting an authentication failure.
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String urlForFindUserByEmail = "http://MEMBER-SERVICE/member/findMemberByEmail";

        // Fetch user details from the external service
        MemberDTO userFoundByEmail;
        try {
            userFoundByEmail = restTemplate.postForObject(urlForFindUserByEmail, email, MemberDTO.class);

            if (userFoundByEmail == null) {
                throw new UserNotFoundException("User is not exist :  email: " + email);
            }
            // Check that userFoundByEmail has the necessary properties
            if (userFoundByEmail.getEmail() == null || userFoundByEmail.getPassword() == null) {
                throw new BadCredentialsException("User details are incomplete: email: " + email);
            }

            // Ensure roles are in the correct format, assuming roles are returned as a comma-separated string
            String[] roles = userFoundByEmail.getRole().split(",");

            // Build UserDetails object to return to SecurityContext
            return User.builder()
                    .username(userFoundByEmail.getEmail())
                    .password(userFoundByEmail.getPassword())
                    .authorities(userFoundByEmail.getRole()) // Ensure roles are in the correct format
                    .build();

        } catch (UserNotFoundException e) {
            log.error("user not found -" + e.toString());
            throw e;
        } catch (RestClientException e) {
            log.error("IllegalStateException" + e.toString());
            throw e;
        } catch (Exception e) {
            log.error("Unknown Exception" + e.toString());
            throw e;
        }


    }
}
