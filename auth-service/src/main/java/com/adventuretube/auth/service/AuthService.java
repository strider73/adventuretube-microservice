package com.adventuretube.auth.service;

import com.adventuretube.auth.config.google.GoogleTokenCredentialProperties;
import com.adventuretube.auth.exceptions.*;
import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.auth.model.mapper.MemberMapper;
import com.adventuretube.auth.model.request.MemberLoginRequest;
import com.adventuretube.auth.model.request.MemberRegisterRequest;
import com.adventuretube.auth.model.response.AuthTokenResponse;
import com.adventuretube.auth.model.dto.member.MemberDTO;
import com.adventuretube.common.client.ServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Base64;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@Service
@Slf4j
public class AuthService {

    @Value("${member-service.url:http://MEMBER-SERVICE}")
    private String memberServiceUrl;
    private final GoogleTokenCredentialProperties googleTokenCredentialProperties;
    private final ServiceClient serviceClient;
    private final JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final PasswordEncoder passwordEncoder;
    private final ReactiveAuthenticationManager reactiveAuthenticationManager;
    private final MemberMapper memberMapper;

    public AuthService(GoogleTokenCredentialProperties googleTokenCredentialProperties,
                       ServiceClient serviceClient,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder,
                       ReactiveAuthenticationManager reactiveAuthenticationManager,
                       MemberMapper memberMapper) {
        this.googleTokenCredentialProperties = googleTokenCredentialProperties;
        this.serviceClient = serviceClient;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.reactiveAuthenticationManager = reactiveAuthenticationManager;
        this.memberMapper = memberMapper;
    }

    public Mono<AuthTokenResponse> createUser(MemberRegisterRequest request) {

        // MARK: Validate Google ID token + build MemberDTO (both blocking — offloaded to boundedElastic)
        return Mono.fromCallable(() -> verifyGoogleIdToken(request.getGoogleIdToken())// return Optional<GoogleIdToken>
                        .map(GoogleIdToken::getPayload)//it becomes Optional<GoogleIdToken.Payload>
                        .orElseThrow(() -> {// it mean Google idToken is null
                            log.error("Google idToken is null");
                            return new GoogleIdTokenInvalidException(AuthErrorCode.GOOGLE_TOKEN_INVALID);
                        }))
                //from here not Optional because of orElseThrow and wrap by Mono  because  of Mono.fromCallable.
                .subscribeOn(Schedulers.boundedElastic())//This method get called from  Mono<GoogleIdToken.Payload> object
                // MARK: Validate email
                .filter(payload -> request.getEmail().equals(payload.getEmail()))//Still get called from Mono<GoogleIdToken.Payload> object
                .switchIfEmpty(Mono.defer(() -> {// failed filter produce empty Mono  not an error
                    //that's why switchIfEmpty is triggered and produce proper Mono.error
                    log.warn("Google email mismatch for request email: {}", request.getEmail());
                    return Mono.error(new GoogleIdTokenInvalidException(AuthErrorCode.GOOGLE_EMAIL_MISMATCH));
                }))
                // MARK: if payload is valid, build MemberDTO
                // passwordEncoder.encode is cpu intensive job so need to offload so now method will return Mono<MemberDTO>
                .flatMap(this::buildMemberDTO)
                // MARK:  Check Email duplication
                //flatMap took memberDTO and  serviceClient.postReactive returns Mono<ServiceResponse<Boolean>>
                .flatMap(memberDTO -> serviceClient.postReactive(// flatMap get call from Mono<MemberDTO>
                                memberServiceUrl,
                                "/member/emailDuplicationCheck",
                                memberDTO.getEmail(),
                                new ParameterizedTypeReference<Boolean>() {}
                )
                 /*
                        flatMap get called from Mono<ServiceResponse<Boolean>>
                        but more important reason why flatMap is used here???
                        It's because serviceClient.postReactive returns Mono<ServiceResponse<Boolean>>
                        if we use maps it will return Mono<Mono<ServiceResponse<MemberDTO>>>

                        So the main point is choosing a map or flatMap will be decided by return type
                */
                .flatMap(emailCheck -> {

                            if (emailCheck == null) {
                                logger.error("Failed to check email duplication");
                                return Mono.error(new InternalServerException(AuthErrorCode.INTERNAL_ERROR));
                            }
                            if (emailCheck) {
                                log.warn("Duplicate email during registration: {}", request.getEmail());
                                return Mono.error(new DuplicateException(AuthErrorCode.USER_EMAIL_DUPLICATE));
                            }
                            // MARK:  Register Member
                            //This will return Mono<MemberDTO>
                            return serviceClient.postReactive(
                                    memberServiceUrl,
                                    "/member/registerMember",
                                    memberDTO,
                                    new ParameterizedTypeReference<MemberDTO>() {}
                            );
                })//because of flatMap it will return Mono<MemberDTO>

                        //MARK:  Register Member process validation and create token and store token to database
                        //flatMap get called from Mono<ServiceResponse<MemberDTO>>
                .flatMap(registeredMemberDTO -> {
                    if (registeredMemberDTO == null) {
                        log.error("Member registration failed for email: {}", request.getEmail());
                        //because of flatMap has to return Mono
                        return Mono.error(new InternalServerException(AuthErrorCode.INTERNAL_ERROR));
                    }

                    // MARK:  create JWT token
                    return Mono.fromCallable(
                                    () -> {
                                        String accessToken = jwtUtil.generate(registeredMemberDTO.getEmail(), registeredMemberDTO.getRole(), "ACCESS");
                                        String refreshToken = jwtUtil.generate(registeredMemberDTO.getEmail(), registeredMemberDTO.getRole(), "REFRESH");
                                        return com.adventuretube.auth.model.dto.token.TokenDTO.builder()
                                                .memberDTO(registeredMemberDTO)
                                                .expired(false)
                                                .revoked(false)
                                                .accessToken(accessToken)
                                                .refreshToken(refreshToken)
                                                .build();
                                    }
                            ).subscribeOn(Schedulers.boundedElastic())//This will return Mono<TokenDTO>
                            .flatMap(tokenDTO -> {
                                return serviceClient.postReactive(
                                                memberServiceUrl,
                                                "/member/storeTokens",
                                                tokenDTO,
                                                new ParameterizedTypeReference<Boolean>() {}
                                        )//validate token store and return Mono<Boolean>
                                        .flatMap(isTokenStored -> {
                                            if (isTokenStored == null || !isTokenStored) {
                                                log.error("Token store failed: {}", isTokenStored != null ? isTokenStored : "no response body");
                                                return Mono.error(new TokenSaveFailedException(AuthErrorCode.TOKEN_SAVE_FAILED));
                                            }
                                            logger.info("Token stored successfully for user: {}", registeredMemberDTO.getEmail());
                                            return Mono.just(new AuthTokenResponse(registeredMemberDTO.getId(), tokenDTO.accessToken, tokenDTO.refreshToken));
                                        });
                            });//This will return Mono<MemberRegisterResponse>


                })//end of  create token and store token to database
        );//end of outer flatMap(memberDTO -> ...)

    }//end of method createUser


    public Mono<AuthTokenResponse> issueToken(MemberLoginRequest request) {

        // MARK: STEP1 validate google IdToken (blocking — offloaded to boundedElastic)
        return Mono.fromCallable(() -> verifyGoogleIdToken(request.getGoogleIdToken())
                    .orElseThrow(() -> {
                        log.error("Invalid Google ID token");
                        return new GoogleIdTokenInvalidException(AuthErrorCode.GOOGLE_TOKEN_INVALID);
                    })
                )//it will return Mono<GoogleIdToken>
                .subscribeOn(Schedulers.boundedElastic())
                // MARK: STEP2 extract email and googleId from validated token
                //1. flatMap get call from Mono<GoogleIdToken>
                //2. it will take GoogleIdToken as parameter
                //3. it will return Mono<ServiceResponse<MemberRegisterResponse>>

                // first level flatMap start
                .flatMap(idToken -> {
                    GoogleIdToken.Payload payload = idToken.getPayload();
                    String email = payload.getEmail();
                    String googleId = payload.getSubject();

                    // MARK: STEP3 authenticate and issue tokens
                    return reactiveAuthenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, googleId))
                           .flatMap(authentication -> {// Second level flatMap start

                                //TODO: this is high cost computation area
                            return Mono.fromCallable(() -> {
                                    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                                    String accessToken = jwtUtil.generate(userDetails.getUsername(), userDetails.getAuthorities().toString(), "ACCESS");
                                    String refreshToken = jwtUtil.generate(userDetails.getUsername(), userDetails.getAuthorities().toString(), "REFRESH");

                                     return com.adventuretube.auth.model.dto.token.TokenDTO.builder()
                                            .memberDTO(memberMapper.userDetailToMemberDTO(userDetails))
                                            .expired(false)
                                            .revoked(false)
                                            .accessToken(accessToken)
                                            .refreshToken(refreshToken)
                                            .build();
                                })//end of Mono.fromCallable
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(tokenDTO -> {//Third level flatMap start
                                    return serviceClient.postReactive(
                                                    memberServiceUrl,
                                                    "/member/storeTokens",
                                                    tokenDTO,
                                                    new ParameterizedTypeReference<Boolean>() {}
                                            )//it will return Mono<Boolean>

                                            //Fourth level flatMap start and flatMap get call from Mono<Boolean>
                                            .flatMap(isTokenStored -> {
                                                if (isTokenStored == null || !isTokenStored) {
                                                    log.error("Token store failed: {}", isTokenStored != null ? isTokenStored : "no response body");
                                                    return Mono.error(new TokenSaveFailedException(AuthErrorCode.TOKEN_SAVE_FAILED));
                                                }
                                                logger.info("Token stored successfully for user: {}", email);
                                                return Mono.just(new AuthTokenResponse(null, tokenDTO.accessToken, tokenDTO.refreshToken));
                                            });//End of fourth level, it will return Mono<MemberRegisterResponse>
                                });//End of third level will return Mono<MemberRegisterResponse>

                           });//end of 2nd level flatMap

                });//end of 1st level flatMap

    }//end of method issueToken


    // JWT token is validated at Gateway (RouterValidator secures /auth/token/revoke)
    public Mono<Boolean> revokeToken(String rawToken) {
        String token = TokenSanitizer.sanitize(rawToken);

        return serviceClient.postReactive(
                        memberServiceUrl,
                        "/member/deleteAllToken",
                        token,
                        new ParameterizedTypeReference<Boolean>() {}
                )
                .map(isDeleteToken -> {
                    if (isDeleteToken == null|| !isDeleteToken){
                        log.warn("Token deletion failed for token: {}", token);
                        throw new TokenDeletionException(AuthErrorCode.TOKEN_DELETION_FAILED);
                    }
                    log.info("Logout has been successful for token: {}", token);
                    return isDeleteToken;
                });
    }

    public Mono<AuthTokenResponse> refreshToken(String rawToken) {
        /*
         * Refresh Token Flow:
         * 1. JWT signature & expiration validated at Gateway (RouterValidator secures /auth/token/refresh)
         * 2. Check token exists in DB (not revoked/logged out)
         * 3. Extract username and role from token, issue new access & refresh tokens
         */
        String token = TokenSanitizer.sanitize(rawToken);
        log.info(">>> refreshToken: validating refresh token against member-service");

        return serviceClient.postReactive(
                        memberServiceUrl,
                        "/member/findToken",
                        token,
                        new ParameterizedTypeReference<Boolean>() {}
                )
                .flatMap(isFindToken -> {
                    if (isFindToken == null|| !isFindToken) {
                        log.warn("Refresh token not found in DB");
                        return Mono.error(new TokenNotFoundException(AuthErrorCode.TOKEN_NOT_FOUND));
                    }

                    return Mono.fromCallable(() -> {
                                String userName = jwtUtil.extractUsername(token);
                                String role = jwtUtil.extractUserRole(token);
                                log.info(">>> refreshToken: token found in DB, issuing new tokens for user: {}", userName);
                                String accessToken = jwtUtil.generate(userName, role, "ACCESS");
                                String refreshToken = jwtUtil.generate(userName, role, "REFRESH");

                                MemberDTO memberDTO = MemberDTO.builder().username(userName).role(role).build();
                                return com.adventuretube.auth.model.dto.token.TokenDTO.builder()
                                        .memberDTO(memberDTO)
                                        .expired(false)
                                        .revoked(false)
                                        .accessToken(accessToken)
                                        .refreshToken(refreshToken)
                                        .build();
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(tokenDTO -> {
                                MemberDTO memberDTO = tokenDTO.getMemberDTO();
                                return serviceClient.postReactive(
                                                memberServiceUrl,
                                                "/member/storeTokens",
                                                tokenDTO,
                                                new ParameterizedTypeReference<Boolean>() {}
                                        )
                                        .flatMap(isTokenStored -> {
                                            if (isTokenStored == null || !isTokenStored) {
                                                log.error("Token store failed: {}", isTokenStored != null ? isTokenStored : "no response body");
                                                return Mono.error(new TokenSaveFailedException(AuthErrorCode.TOKEN_SAVE_FAILED));
                                            }
                                            log.info(">>> refreshToken: new tokens issued and stored successfully for user: {}", memberDTO.getUsername());
                                            return Mono.just(new AuthTokenResponse(null, tokenDTO.accessToken, tokenDTO.refreshToken));
                                        });
                            });
                });
    }


    public Mono<Boolean> deleteUser(String email) {
        return serviceClient.postReactive(
                        memberServiceUrl,
                        "/member/deleteUser",
                        email,
                        new ParameterizedTypeReference<Boolean>() {}
                )
                .map(isDeleteUser -> {
                    if (isDeleteUser == null|| !isDeleteUser) {
                        log.error("Member deletion failed for email: {}", email);
                        throw new InternalServerException(AuthErrorCode.MEMBER_DELETION_FAILED);
                    }
                    logger.info("User deleted successfully: {}", email);
                    return isDeleteUser;
                })
;
    }

    protected Optional<GoogleIdToken> verifyGoogleIdToken(String googleIdToken) {
        // First decode the token to check the audience claim
        try {
            String[] chunks = googleIdToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(chunks[1]));

            // Parse the JSON payload to check the 'aud' field
            ObjectMapper mapper = new ObjectMapper();
            JsonNode tokenPayload = mapper.readTree(payload);
            String tokenAudience = tokenPayload.get("aud").asText();
            String expectedClientId = googleTokenCredentialProperties.getClientId();

            log.debug("Token audience: {}", tokenAudience);
            log.debug("Expected client ID: {}", expectedClientId);

            if (!expectedClientId.equals(tokenAudience)) {
                log.error("Client ID mismatch. Expected: {}, Got: {}", expectedClientId, tokenAudience);
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("Error parsing token payload: {}", e.getMessage());
            return Optional.empty();
        }

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleTokenCredentialProperties.getClientId())).build();

        try {
            return Optional.ofNullable(verifier.verify(googleIdToken));
        } catch (GeneralSecurityException | IOException ex) {
            log.error("Google ID token verification failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Mono<MemberDTO> buildMemberDTO(GoogleIdToken.Payload payload) {
       return  Mono.fromCallable( () -> {
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

        })
        .subscribeOn(Schedulers.boundedElastic());


    }

}
