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
        return serviceClient.postReactive(
                        memberServiceUrl,
                        "/member/findMemberByEmail",
                        email,
                        new ParameterizedTypeReference<ServiceResponse<MemberDTO>>() {}
                )
                .flatMap(response -> Mono.justOrEmpty(response.getData())
                        .switchIfEmpty(Mono.error(new UserNotFoundException(AuthErrorCode.USER_NOT_FOUND)))
                        .flatMap(userFoundByEmail -> {
                            if (userFoundByEmail.getEmail() == null || userFoundByEmail.getPassword() == null) {
                                log.warn("User details incomplete for email: {}", email);
                                return Mono.error(new BadCredentialsException("User details are incomplete: email: " + email));
                            }

                            UserDetails userDetails = User.builder()
                                    .username(userFoundByEmail.getEmail())
                                    .password(userFoundByEmail.getPassword())
                                    .authorities(userFoundByEmail.getRole())
                                    .build();

                            return Mono.just(userDetails);
                        }))
                .onErrorMap(ServiceClientException.class, e -> {
                    if (e.isServerError()) {
                        return e; // return 5XX propagate to the GlobalExceptionHandler as-is
                    }
                    if (e.getHttpStatus() == 404) {
                        log.warn("User not found for email: {}", email);
                        return new UserNotFoundException(AuthErrorCode.USER_NOT_FOUND, e);
                    }
                    // Other 4XX — these are our bugs or auth/rate-limit issues, not "user not found"
                    log.error("Unexpected client error from user service for email {}", email, e);
                    return e;  // propagate; let global handler classify it
                });
    }
}
