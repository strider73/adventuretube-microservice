package com.adventuretube.auth.integration.isolated;

import com.adventuretube.auth.config.security.AuthServiceConfig;
import com.adventuretube.auth.model.dto.member.MemberDTO;
import com.adventuretube.common.api.response.ServiceResponse;
import com.adventuretube.common.client.ServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("mock")
@Import(AuthServiceConfig.class)
public class CustomUserDetailServiceSecurityIT {

    @MockBean
    private ServiceClient serviceClient;

    @Autowired
    private ReactiveAuthenticationManager reactiveAuthenticationManager;

    @Test
    void authenticateUser_shouldSucceed_whenValidCredentials() {
        // given
        String email = "security@example.com";
        String password = "securePassword";

        MemberDTO mockMember = new MemberDTO();
        mockMember.setEmail(email);
        mockMember.setPassword(new BCryptPasswordEncoder().encode(password));
        mockMember.setRole("ROLE_USER");

        ServiceResponse<MemberDTO> response = new ServiceResponse<>();
        response.setSuccess(true);
        response.setData(mockMember);

        when(serviceClient.postReactive(
                eq("http://MEMBER-SERVICE"),
                eq("/member/findMemberByEmail"),
                eq(email),
                any(ParameterizedTypeReference.class)
        )).thenReturn(Mono.just(response));

        // when
        Authentication authRequest = new UsernamePasswordAuthenticationToken(email, password);
        Mono<Authentication> authResult = reactiveAuthenticationManager.authenticate(authRequest);

        // then
        StepVerifier.create(authResult)
                .assertNext(auth -> {
                    assertThat(auth.isAuthenticated()).isTrue();
                    UserDetails principal = (UserDetails) auth.getPrincipal();
                    assertThat(principal.getUsername()).isEqualTo(email);
                })
                .verifyComplete();
    }
}
