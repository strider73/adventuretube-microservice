package com.adventuretube.auth.service;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.auth.exceptions.UserNotFoundException;
import com.adventuretube.auth.model.dto.member.MemberDTO;
import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.common.client.ServiceClient;
import com.adventuretube.common.client.ServiceClientException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private static final String MEMBER_SERVICE_URL = "http://MEMBER-SERVICE";

    private final ServiceClient serviceClient;

    //UsernameNotFoundException will be handled by Spring Security resulting an authentication failure.
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            ServiceResponse<MemberDTO> response = serviceClient.post(
                    MEMBER_SERVICE_URL,
                    "/member/findMemberByEmail",
                    email,
                    new ParameterizedTypeReference<ServiceResponse<MemberDTO>>() {}
            );

            if (response == null || !response.isSuccess()) {
                throw new UserNotFoundException(AuthErrorCode.USER_NOT_FOUND);
            }

            MemberDTO userFoundByEmail = response.getData();
            if (userFoundByEmail == null) {
                throw new UserNotFoundException(AuthErrorCode.USER_NOT_FOUND);
            }

            // Check that userFoundByEmail has the necessary properties
            if (userFoundByEmail.getEmail() == null || userFoundByEmail.getPassword() == null) {
                throw new BadCredentialsException("User details are incomplete: email: " + email);
            }

            // Build UserDetails object to return to SecurityContext
            return User.builder()
                    .username(userFoundByEmail.getEmail())
                    .password(userFoundByEmail.getPassword())
                    .authorities(userFoundByEmail.getRole())
                    .build();

        } catch (UserNotFoundException e) {
            log.error("User not found: {}", e.toString());
            throw e;
        } catch (ServiceClientException e) {
            log.error("Service client error: {}", e.toString());
            throw new UserNotFoundException(AuthErrorCode.USER_NOT_FOUND);
        } catch (Exception e) {
            log.error("Unknown exception: {}", e.toString());
            throw e;
        }
    }
}
