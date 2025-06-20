package com.adventuretube.auth.service;

import com.adventuretube.auth.config.google.GoogleTokenCredentialProperties;
import com.adventuretube.auth.exceptions.*;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.auth.model.mapper.MemberMapper;
import com.adventuretube.auth.model.request.MemberLoginRequest;
import com.adventuretube.auth.common.response.RestAPIResponse;
import com.adventuretube.auth.model.request.MemberRegisterRequest;
import com.adventuretube.auth.model.response.MemberRegisterResponse;
import com.adventuretube.auth.model.dto.member.MemberDTO;
import com.adventuretube.auth.model.dto.token.TokenDTO;
import com.adventuretube.common.api.response.ServiceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
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

    private final GoogleTokenCredentialProperties googleTokenCredentialProperties;
    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final MemberMapper memberMapper;

    @Transactional
    public MemberRegisterResponse createUser(MemberRegisterRequest request) {

        // MARK:   Validate Google ID token  https://developers.google.com/identity/sign-in/ios/backend-auth
        GoogleIdToken idToken = verifyGoogleIdToken(request.getGoogleIdToken());
        if (idToken == null) {
            log.error("Google idToken is null");
            throw new GoogleIdTokenInvalidException(AuthErrorCode.GOOGLE_TOKEN_INVALID);
        }
        GoogleIdToken.Payload payload = idToken.getPayload();
        // MARK:  compare email address that in the request with payload
        if (!request.getEmail().equals(payload.getEmail())) {
            throw new GoogleIdTokenInvalidException(AuthErrorCode.GOOGLE_EMAIL_MISMATCH);
        }


        // MARK:  prepare check user email duplication
        MemberDTO memberDTO = buildMemberDTO(payload);
        String urlForEmailCheck = "http://MEMBER-SERVICE/member/emailDuplicationCheck";
        ResponseEntity<ServiceResponse<Boolean>> response = restTemplate.exchange(
                urlForEmailCheck,
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(memberDTO.getEmail()),
                new ParameterizedTypeReference<ServiceResponse<Boolean>>() {
                }
        );

        ServiceResponse<Boolean> body = response.getBody();
        if(!response.getStatusCode().is2xxSuccessful()
                || body == null
                || !body.isSuccess()) {
            logger.error("Failed to check email duplication: {}", response.getStatusCode());
            throw new MemberServiceException(AuthErrorCode.INTERNAL_ERROR);
        }
        // MARK:  Check Email duplication
        if (Boolean.TRUE.equals(body.getData())) {
            throw new DuplicateException(AuthErrorCode.USER_EMAIL_DUPLICATE);
        }


        try {
            // MARK:  Register Member !!!
            String urlForRegister = "http://MEMBER-SERVICE/member/registerMember"; //with Eureka
            ResponseEntity<ServiceResponse<MemberDTO>> registerMemberResponse = restTemplate.exchange(
                    urlForRegister,
                    org.springframework.http.HttpMethod.POST,
                    new HttpEntity<>(memberDTO),
                    new ParameterizedTypeReference<ServiceResponse<MemberDTO>>() {}
            );
            if (!registerMemberResponse.getStatusCode().is2xxSuccessful()) {
                throw new MemberServiceException(AuthErrorCode.INTERNAL_ERROR);
            }

           ServiceResponse<MemberDTO> registerMemberResponseBody = registerMemberResponse.getBody();
            // MARK:  create JWT token
            MemberDTO registeredUser = registerMemberResponseBody.getData();
            String accessToken = jwtUtil.generate(registeredUser.getEmail(), registeredUser.getRole(), "ACCESS");
            String refreshToken = jwtUtil.generate(registeredUser.getEmail(), registeredUser.getRole(), "REFRESH");
            //TODO saveToken and revoke all others
            String urlForStoreToken = "http://MEMBER-SERVICE/member/storeTokens";
                 /*
                    Member object which implement UserDetail have a issue with serial/deserialization GrantedAuthority Object
                    so sending MemberDTO instead and convert to Member in member-service
                  */
            // MARK:   store token to database
            TokenDTO tokenToStore = TokenDTO.builder()
                    .memberDTO(registeredUser)//sending a memberDTO instead Member
                    .expired(false)
                    .revoked(false)
                    .accessToken(accessToken)
                    .refreshToken(refreshToken) // Set refresh token to null or generate if needed
                    .build();

            ResponseEntity<ServiceResponse<Boolean>> tokenStoredResponse = restTemplate.exchange(
                    urlForStoreToken,
                    org.springframework.http.HttpMethod.POST,
                    new HttpEntity<>(tokenToStore),
                    new ParameterizedTypeReference<ServiceResponse<Boolean>>() {}
            );
            ServiceResponse<Boolean> tokenStoredResponseBody = tokenStoredResponse.getBody();


            if (!tokenStoredResponse.getStatusCode().is2xxSuccessful()
                    || tokenStoredResponseBody == null
                    || !tokenStoredResponseBody.isSuccess()
                    || !Boolean.TRUE.equals(tokenStoredResponseBody.getData())) {
                log.error("Token store failed: {}", tokenStoredResponseBody != null ? tokenStoredResponseBody.getMessage() : "no response body");
                throw new TokenSaveFailedException(AuthErrorCode.TOKEN_SAVE_FAILED);
            }
            logger.info("Token stored successfully for user: {}", registeredUser.getEmail());
            // MARK:  return result

            return new MemberRegisterResponse(registeredUser.getId(), accessToken, refreshToken);

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            // Parse error response body
            try {
                RestAPIResponse errorResponse = new ObjectMapper().readValue(ex.getResponseBodyAsString(), RestAPIResponse.class);
                logger.error("Member service error: {} - {}", errorResponse.getStatusCode(), errorResponse.getMessage());
                throw new RuntimeException(errorResponse.getMessage());
                //TODO:  need to create a switch exception for each error code after add errorCode property to RestAPIResponse
                /*
                switch (code) {
                case USER_EMAIL_DUPLICATE -> throw new DuplicateException(code);
                case TOKEN_SAVE_FAILED     -> throw new TokenSaveFailedException(code);
                case GOOGLE_TOKEN_INVALID,
                     GOOGLE_EMAIL_MISMATCH -> throw new GoogleIdTokenInvalidException(code);
                default                    -> throw new MemberServiceException(code);
                }
                 */
            } catch (Exception e) {
                logger.error("Error parsing error response", e);
                throw new MemberServiceException(AuthErrorCode.INTERNAL_ERROR);
            }
        } catch (Exception ex) {
            logger.error("An unexpected error occurred during member registration", ex);
            throw new MemberServiceException(AuthErrorCode.INTERNAL_ERROR);
        }

    }


    public MemberRegisterResponse issueToken(MemberLoginRequest request) {

        //MARK: STEP1 validate google IdToken
        GoogleIdToken idToken = verifyGoogleIdToken(request.getGoogleIdToken());
        if (idToken == null) {
            log.error("Invalid Google ID token");
            throw new GoogleIdTokenInvalidException(AuthErrorCode.GOOGLE_TOKEN_INVALID);
        }

        // MARK: STEP2 prepare check user email duplication
        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String googleId = payload.getSubject();

        //MARK: STEP3 compare email address that in the request with payload
        //Since this request does not yet have a JWT token, it goes through authentication process.
        //and issue the tokens if the  email and googleId are matched using CustomUserDetailsService,
        //which is registered through Security configuration (AuthServiceConfig in this case).
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, googleId));
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtUtil.generate(userDetails.getUsername(), userDetails.getAuthorities().toString(), "ACCESS");
        String refreshToken = jwtUtil.generate(userDetails.getUsername(), userDetails.getAuthorities().toString(), "REFRESH");


        TokenDTO tokenToStore = TokenDTO.builder()
                .memberDTO(memberMapper.userDetailToMemberDTO(userDetails))//sending a memberDTO instead Member
                .expired(false)
                .revoked(false)
                .accessToken(accessToken)
                .refreshToken(refreshToken) // Set refresh token to null or generate if needed
                .build();

        String urlForStoreToken = "http://MEMBER-SERVICE/member/storeTokens";
        ResponseEntity<ServiceResponse<Boolean>> tokenToStoreResponse = restTemplate.exchange(
                urlForStoreToken,
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(tokenToStore),
                new ParameterizedTypeReference<ServiceResponse<Boolean>>() {}
        );


        ServiceResponse<Boolean> tokenStoredResponseBody = tokenToStoreResponse.getBody();


        if (!tokenToStoreResponse.getStatusCode().is2xxSuccessful()
                || tokenStoredResponseBody == null
                || !tokenStoredResponseBody.isSuccess()
                || !Boolean.TRUE.equals(tokenStoredResponseBody.getData())) {
            log.error("Token store failed: {}", tokenStoredResponseBody != null ? tokenStoredResponseBody.getMessage() : "no response body");
            throw new TokenSaveFailedException(AuthErrorCode.TOKEN_SAVE_FAILED);
        }
        logger.info("Token stored successfully for user: {}", email);

        return new MemberRegisterResponse(null, accessToken, refreshToken);
    }


    public RestAPIResponse revokeToken(HttpServletRequest httpServletRequest) {
        String token = TokenSanitizer.sanitize(httpServletRequest.getHeader("Authorization")); // Assuming the token is passed in the Authorization header
        //using access token for logout
        String urlForDeleteToken = "http://MEMBER-SERVICE/member/deleteAllToken";
        Boolean isLoggedOut = restTemplate.postForObject(urlForDeleteToken, token, Boolean.class);
        if (!isLoggedOut) {
            throw new TokenDeletionException(AuthErrorCode.TOKEN_DELETION_FAILED);
        } else {
            return RestAPIResponse.builder()
                    .message("Logout has been successful")
                    .details("AuthService.logout() : auth-service ")
                    .statusCode(HttpStatus.OK.value())
                    .timestamp(System.currentTimeMillis()).build();
        }

    }

    @Transactional
    public MemberRegisterResponse refreshToken(HttpServletRequest httpServletRequest) {

               /*TODO List
       1. get the refresh token
       2. it been already do basic validate from gateway
       3. and also did user name check from JwtAuthFilter  since /refreshToken is not an exception from  SecurityServiceConfig
       4. so get the username and role  from the token and create userDetail
        */

        String token = TokenSanitizer.sanitize(httpServletRequest.getHeader("Authorization")); // Assuming the token is passed in the Authorization header

        //check the token for logout
        String urlForTokenExist = "http://MEMBER-SERVICE/member/findToken";
        Boolean isTokenFind = restTemplate.postForObject(urlForTokenExist, token, Boolean.class);
        if (!isTokenFind) {
            throw new TokenNotFoundException(AuthErrorCode.TOKEN_NOT_FOUND);
        }


        String userName = jwtUtil.extractUsername(token);
        String role = jwtUtil.extractUserRole(token);

        String accessToken = jwtUtil.generate(userName, role, "ACCESS");
        String refreshToken = jwtUtil.generate(userName, role, "REFRESH");

        MemberDTO memberDTO = MemberDTO.builder().username(userName).role(role).build();


        TokenDTO tokenToStore = TokenDTO.builder()
                .memberDTO(memberDTO)//sending a memberDTO instead Member
                .expired(false)
                .revoked(false)
                .accessToken(accessToken)
                .refreshToken(refreshToken) // Set refresh token to null or generate if needed
                .build();

        String urlForStoreToken = "http://MEMBER-SERVICE/member/storeTokens";
        ResponseEntity<ServiceResponse<Boolean>> tokenStoredResponse = restTemplate.exchange(
                urlForStoreToken,
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(tokenToStore),
                new ParameterizedTypeReference<ServiceResponse<Boolean>>() {}
        );
        ServiceResponse<Boolean> tokenStoredResponseBody = tokenStoredResponse.getBody();
        if (!tokenStoredResponse.getStatusCode().is2xxSuccessful()
                || tokenStoredResponseBody == null
                || !tokenStoredResponseBody.isSuccess()
                || !Boolean.TRUE.equals(tokenStoredResponseBody.getData())) {
            log.error("Token store failed: {}", tokenStoredResponseBody != null ? tokenStoredResponseBody.getMessage() : "no response body");
            throw new TokenSaveFailedException(AuthErrorCode.TOKEN_SAVE_FAILED);
        }

        return new MemberRegisterResponse(null, accessToken, refreshToken);
    }


    private GoogleIdToken verifyGoogleIdToken(String googleIdToken) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance()).setAudience(Collections.singletonList(googleTokenCredentialProperties.getClientId())).build();

        try {
            return verifier.verify(googleIdToken);
        } catch (GeneralSecurityException | IOException ex) {
            log.error("Google ID token verification failed: {}", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    private MemberDTO buildMemberDTO(GoogleIdToken.Payload payload) {
        String email = payload.getEmail();
        String googleId = payload.getSubject();
        String username = (String) payload.get("name");

        if (username == null || username.isEmpty()) {
            String givenName = (String) payload.get("given_name");
            String familyName = (String) payload.get("family_name");
            username = givenName + " " + familyName;
        }

        return MemberDTO.builder()
                .email(email)
                .googleIdToken(payload.toString())
                .username(username)
                .password(passwordEncoder.encode(googleId))
                .googleIdTokenExp(payload.getExpirationTimeSeconds())
                .googleIdTokenIat(payload.getIssuedAtTimeSeconds())
                .googleIdTokenSub(googleId)
                .googleProfilePicture((String) payload.get("picture")).role("USER")
                .build();
    }

}
