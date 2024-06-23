package com.adventuretube.auth.service;

import com.adventuretube.common.domain.dto.member.MemberDTO;
import com.adventuretube.common.domain.dto.token.TokenDTO;
import com.adventuretube.common.error.RestAPIErrorResponse;
import com.adventuretube.auth.exceptions.DuplicateException;
import com.adventuretube.auth.exceptions.GoogleIdTokenInvalidException;
import com.adventuretube.auth.model.MemberRegisterRequest;
import com.adventuretube.auth.model.MemberRegisterResponse;
import com.adventuretube.auth.mapper.MemberMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;


@Service
@Slf4j
@Transactional(readOnly = true)
@AllArgsConstructor
public class AuthService {

    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
     public MemberRegisterResponse register(MemberRegisterRequest request) {

         // Validate Google ID token
         GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),GsonFactory.getDefaultInstance())
                 .setAudience(Collections.singletonList("657433323337-t5e70nbjmink2ldmt3e34pci55v3sv6k.apps.googleusercontent.com"))
                         .build();
         GoogleIdToken idToken = null;
         try {
             idToken = verifier.verify(request.getGoogleIdToken());
         } catch (GeneralSecurityException | IOException ex) {
             log.error("Google idToken verify has been failed : "+ex.getMessage());
             throw new RuntimeException(ex);
         }
         if (idToken == null) {
             log.error("Google idToken is null");
             throw new GoogleIdTokenInvalidException("Invalid Google ID token.");
         }

         //extract user information
         GoogleIdToken.Payload payload = idToken.getPayload();
         String email = payload.getEmail();
         boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());

         if (!emailVerified || !email.equals(request.getEmail())) {
             throw new GoogleIdTokenInvalidException("Email in Google ID token does not match the provided email or is not verified.");
         }

         // Generate a placeholder password using a hash of the Google ID token's subject
         String googleId = payload.getSubject();
         String placeholderPassword = null;
         try {
             placeholderPassword = passwordEncoder.encode(googleId);
         } catch (Exception e) {
             throw new RuntimeException(e);
         }

         // Set the placeholder password
         request.setPassword(placeholderPassword);

        // Set User Data Transfer Object
        MemberDTO memberDTO = MemberMapper.INSTANCE.memberRegisterRequestToMemberDTO(request);
        memberDTO.setGoogleIdTokenExp(payload.getExpirationTimeSeconds());
        memberDTO.setGoogleIdTokenIat(payload.getIssuedAtTimeSeconds());
        memberDTO.setGoogleIdTokenSub(payload.getSubject());
        memberDTO.setGoogleProfilePicture(payload.get("picture").toString());


        String urlForEmailCheck = "http://MEMBER-SERVICE/member/emailDuplicationCheck";
        Boolean isUserAlreadyExist  = restTemplate.postForObject(urlForEmailCheck, memberDTO.getEmail(),Boolean.class);

        //Check Email duplication
        if(isUserAlreadyExist){
            throw new DuplicateException(String.format("User with the email address '%s' already exists.", request.getEmail()));
        }



        try{
            //Register Member !!!
            String urlForRegister = "http://MEMBER-SERVICE/member/registerMember"; //with Eureka
            ResponseEntity<MemberDTO> response = restTemplate.postForEntity(urlForRegister, memberDTO, MemberDTO.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                MemberDTO registeredUser = response.getBody();
                    String accessToken = jwtUtil.generate(registeredUser.getEmail(), registeredUser.getRole(), "ACCESS");
                    String refreshToken = jwtUtil.generate(registeredUser.getEmail(), registeredUser.getRole(), "REFRESH");
                    //TODO saveToken and revoke all others
                     String urlForStoreToken ="http://MEMBER-SERVICE/member/storeTokens";
                 /*
                    Member object which implement UserDetail have a issue with serial/deserialization GrantedAuthority Object
                    so sending MemberDTO instead and convert to Member in member-service
                  */
                // Prepare tokens to store
                TokenDTO tokenToStore = TokenDTO.builder()
                        .memberDTO(registeredUser)//sending a memberDTO instead Member
                        .expired(false)
                        .revoked(false)
                        .accessToken(accessToken)
                        .refreshToken(refreshToken) // Set refresh token to null or generate if needed
                        .build();



                     Boolean istokenStored = restTemplate.postForObject(urlForStoreToken,tokenToStore, Boolean.class);
                     if(!istokenStored){
                         throw new RuntimeException("token store error !!!");
                     }



                    return new MemberRegisterResponse(memberDTO, accessToken, refreshToken);
            } else {
                // Handle non-200 responses
                String errorBody = response.hasBody() ? response.getBody().toString() : "No response body";
                logger.error("Member service returned an error: HTTP {} - {}", response.getStatusCode(), errorBody);
                throw new RuntimeException("Member service error: " + response.getStatusCode());
            }//handle 4XX Client error  and 5XX  Server Error
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            // Parse error response body
            try {
                RestAPIErrorResponse errorResponse = new ObjectMapper().readValue(ex.getResponseBodyAsString(), RestAPIErrorResponse.class);
                logger.error("Member service error: {} - {}", errorResponse.getStatusCode(), errorResponse.getMessage());
                throw new RuntimeException(errorResponse.getMessage());
            } catch (Exception e) {
                logger.error("Error parsing error response", e);
                throw new RuntimeException("Member service error: " + ex.getStatusCode(), ex);
            }
        } catch (Exception ex) {
            logger.error("An unexpected error occurred during member registration", ex);
            throw new RuntimeException("An unexpected error occurred during member registration", ex);
        }

    }


    public MemberRegisterResponse authenticate(MemberRegisterRequest request){

        //Authenticate the user
        //Since this request haven't any token to carry
        //it will go through authentication process and issue the tokens

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtUtil.generate(userDetails.getUsername(),userDetails.getAuthorities().toString(), "ACCESS");
        String refreshToken = jwtUtil.generate(userDetails.getUsername(),userDetails.getAuthorities().toString(), "REFRESH");

        //TODO save token and revoke all the others

        MemberDTO memberDTO = MemberMapper.INSTANCE.userDetailToMemberDTO(userDetails);



        return new MemberRegisterResponse(memberDTO,accessToken, refreshToken);
    }

    public MemberRegisterResponse refreshToken (HttpServletRequest httpServletRequest){

               /*TODO List
       1. get the refresh token
       2. it been already do basic validate from gateway
       3. and also did user name check from JwtAuthFilter  since /refreshToken is not an exception from  SecurityServiceConfig
       4. so get the username and role  from the token and create userDetail
        */

        String token = httpServletRequest.getHeader("Authorization"); // Assuming the token is passed in the Authorization header
        String userName = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractUserRole(token);

        String accessToken = jwtUtil.generate(userName,role,"ACCESS");
        String refreshToken = jwtUtil.generate(userName,role, "REFRESH");

        MemberDTO memberDTO = MemberDTO.builder()
                .username(userName)
                .role(role)
                .build();

        return new MemberRegisterResponse(memberDTO,accessToken, refreshToken);
    }
}
