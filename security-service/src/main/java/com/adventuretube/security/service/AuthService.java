package com.adventuretube.security.service;

import com.adventuretube.common.domain.dto.auth.AuthDTO;
import com.adventuretube.common.error.RestAPIErrorResponse;
import com.adventuretube.security.exceptions.DuplicateException;
import com.adventuretube.security.exceptions.GoogleIdTokenInvalidException;
import com.adventuretube.security.model.AuthRequest;
import com.adventuretube.security.model.AuthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
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
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;


@Service
@Slf4j
@Transactional(readOnly = true)
@AllArgsConstructor
public class AuthService {

    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Transactional
     public AuthResponse register(AuthRequest request) {

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
             placeholderPassword = hashGoogleId(googleId);
         } catch (Exception e) {
             throw new RuntimeException(e);
         }

         // Set the placeholder password
         request.setPassword(placeholderPassword);

         //Set User Data Transfer Object
         AuthDTO authDTO =  AuthDTO.builder()
                                   .googleIdTokenExp(payload.getExpirationTimeSeconds())
                                   .googleIdTokenIat(payload.getIssuedAtTimeSeconds())
                                   .googleIdTokenSub(payload.getSubject())
                                   .googleProfilePicture(payload.get("picture").toString())
                                   .build();
         BeanUtils.copyProperties(request,authDTO);


        String urlForEmailCheck = "http://MEMBER-SERVICE/member/emailDuplicationCheck";
        Boolean isUserAlreadyExist  = restTemplate.postForObject(urlForEmailCheck,authDTO.getEmail(),Boolean.class);

        //Check Email duplication
        if(isUserAlreadyExist){
            throw new DuplicateException(String.format("User with the email address '%s' already exists.", request.getEmail()));
        }



        try{
            //Register Member !!!
            String urlForRegister = "http://MEMBER-SERVICE/member/registerMember"; //with Eureka
            ResponseEntity<AuthDTO> response = restTemplate.postForEntity(urlForRegister, authDTO, AuthDTO.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                AuthDTO registeredUser = response.getBody();
                    String accessToken = jwtUtil.generate(registeredUser.getEmail(), registeredUser.getRole(), "ACCESS");
                    String refreshToken = jwtUtil.generate(registeredUser.getEmail(), registeredUser.getRole(), "REFRESH");
                    return new AuthResponse(accessToken, refreshToken);
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

    private String hashGoogleId(String googleId) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(googleId.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hash);
    }

    public  AuthResponse getToken(UserDetails userDetails){

        String accessToken = jwtUtil.generate(userDetails.getUsername(),userDetails.getAuthorities().toString(), "ACCESS");
        String refreshToken = jwtUtil.generate(userDetails.getUsername(),userDetails.getAuthorities().toString(), "REFRESH");


        return new AuthResponse(accessToken, refreshToken);
     }
}
