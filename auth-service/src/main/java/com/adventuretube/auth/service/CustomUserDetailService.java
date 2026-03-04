package com.adventuretube.auth.service;

import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.auth.exceptions.UserNotFoundException;
import com.adventuretube.auth.model.dto.member.MemberDTO;
import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.common.client.ServiceClient;
import com.adventuretube.common.client.ServiceClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class CustomUserDetailService implements ReactiveUserDetailsService {
    @Value("${member-service.url:http://MEMBER-SERVICE}")
    private String memberServiceUrl;

    private final ServiceClient serviceClient;

    public CustomUserDetailService(ServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    @Override
    public Mono<UserDetails> findByUsername(String email) {
        return serviceClient.postServiceResponseReactive(
                        memberServiceUrl,
                        "/member/findMemberByEmail",
                        email,
                        new ParameterizedTypeReference<ServiceResponse<MemberDTO>>() {}
                )
                .flatMap(response -> {
                    if (response == null || !response.isSuccess()) {
                        return Mono.error(new UserNotFoundException(AuthErrorCode.USER_NOT_FOUND));
                    }

                    MemberDTO userFoundByEmail = response.getData();
                    if (userFoundByEmail == null) {
                        return Mono.error(new UserNotFoundException(AuthErrorCode.USER_NOT_FOUND));
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
                    return new UserNotFoundException(AuthErrorCode.USER_NOT_FOUND);
                });
    }
}
