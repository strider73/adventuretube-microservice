package com.adventuretube.geospatial.service;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.geospatial.model.dto.MemberDTO;
import com.adventuretube.geospatial.exceptions.UserNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
            ResponseEntity<ServiceResponse<MemberDTO>> userFindByEmailResponse = restTemplate.exchange(
                    urlForFindUserByEmail,
                    org.springframework.http.HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(email),
                    new org.springframework.core.ParameterizedTypeReference<ServiceResponse<MemberDTO>>() {}
            );

            ServiceResponse<MemberDTO> findByEmailResponseBody = userFindByEmailResponse.getBody();

            if (findByEmailResponseBody == null || !findByEmailResponseBody.isSuccess()) {
                throw new UserNotFoundException("User not found with email: " + email);
            }
            userFoundByEmail = findByEmailResponseBody.getData();
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
