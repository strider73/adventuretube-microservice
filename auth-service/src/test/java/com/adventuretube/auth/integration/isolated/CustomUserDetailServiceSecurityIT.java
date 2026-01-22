package com.adventuretube.auth.mockhttp.service;

import com.adventuretube.auth.config.security.AuthServiceConfig;
import com.adventuretube.auth.model.dto.member.MemberDTO;
import com.adventuretube.auth.service.CustomUserDetailService;
import com.adventuretube.common.api.response.ServiceResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@SpringBootTest
@ActiveProfiles("mock")
@Import(AuthServiceConfig.class)
public class CustomUserDetailServiceSecurityIT {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AuthenticationManager authenticationManager;

    private MockRestServiceServer mockServer;

    @Test
    void authenticateUser_shouldSucceed_whenValidCredentials() throws Exception {
        mockServer = MockRestServiceServer.createServer(restTemplate);

        // given
        String email = "security@example.com";
        String password = "securePassword";
        //create a mock MemberDTO to simulate the response from the member service
        MemberDTO mockMember = new MemberDTO();
        mockMember.setEmail(email);
        mockMember.setPassword(new BCryptPasswordEncoder().encode(password));
        mockMember.setRole("ROLE_USER");

        //create a mock ServiceResponse to simulate the response using mockMember
        ServiceResponse<MemberDTO> response = new ServiceResponse<>();
        response.setSuccess(true);
        response.setData(mockMember);

        // Serialize mock response
        String jsonResponse;
        try {
            jsonResponse = new MappingJackson2HttpMessageConverter()
                    .getObjectMapper()
                    .writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize mock response", e);
        }



//        for (int i = 0; i < 2; i++) {
            mockServer.expect(requestTo("http://MEMBER-SERVICE/member/findMemberByEmail"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().string(email))
                    .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));
        //}


        // when
        Authentication authRequest = new UsernamePasswordAuthenticationToken(email, password);
        Authentication authResult = authenticationManager.authenticate(authRequest);

        // then
        assertThat(authResult.isAuthenticated()).isTrue();
        UserDetails principal = (UserDetails) authResult.getPrincipal();
        assertThat(principal.getUsername()).isEqualTo(email);
        //assertThat(principal.getPassword()).isEqualTo(password);

        mockServer.verify();
    }
}
