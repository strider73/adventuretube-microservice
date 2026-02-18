package com.adventuretube.geospatial.service;

import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.geospatial.model.dto.MemberDTO;
import com.adventuretube.geospatial.exceptions.UserNotFoundException;
import com.adventuretube.common.client.ServiceClient;
import com.adventuretube.common.client.ServiceClientException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class CustomUserDetailService implements ReactiveUserDetailsService {
    private static final String MEMBER_SERVICE_URL = "http://MEMBER-SERVICE";

    private final ServiceClient serviceClient;

    @Override
    public Mono<UserDetails> findByUsername(String email) {
        return serviceClient.postReactive(
                        MEMBER_SERVICE_URL,
                        "/member/findMemberByEmail",
                        email,
                        new ParameterizedTypeReference<ServiceResponse<MemberDTO>>() {}
                )
                .flatMap(response -> {
                    if (response == null || !response.isSuccess()) {
                        return Mono.error(new UserNotFoundException("User not found with email: " + email));
                    }

                    MemberDTO userFoundByEmail = response.getData();
                    if (userFoundByEmail == null) {
                        return Mono.error(new UserNotFoundException("User not found with email: " + email));
                    }

                    if (userFoundByEmail.getEmail() == null || userFoundByEmail.getPassword() == null) {
                        return Mono.error(new BadCredentialsException("User details are incomplete: email: " + email));
                    }

                    UserDetails userDetails = User.builder()
                            .username(userFoundByEmail.getEmail())
                            .password(userFoundByEmail.getPassword())
                            .authorities(userFoundByEmail.getRole())
                            .build();

                    return Mono.just(userDetails);
                })
                .onErrorMap(ServiceClientException.class, e -> {
                    log.error("Service client error: {}", e.toString());
                    return new UserNotFoundException("User not found with email: " + email);
                });
    }
}
