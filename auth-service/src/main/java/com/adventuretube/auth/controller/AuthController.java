package com.adventuretube.auth.controller;


import com.adventuretube.common.error.RestAPIResponse;
import com.adventuretube.auth.model.MemberRegisterRequest;
import com.adventuretube.auth.model.MemberRegisterResponse;
import com.adventuretube.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;


    @Operation(
            summary = "Signup  new user",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Signup Example",
                                    value = """
                                            {
                                              "googleIdToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjgyMWYzYmM2NmYwNzUxZjc4NDA2MDY3OTliMWFkZjllOWZiNjBkZmIiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI2NTc0MzMzMjMzMzctN2dlMzc1ODBsZGtqczNpMTNycW4ycGMydmFmNjFrcGQuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI2NTc0MzMzMjMzMzctN2dlMzc1ODBsZGtqczNpMTNycW4ycGMydmFmNjFrcGQuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMTA4MTQ5NzI0OTUwMjgwOTM1NDkiLCJlbWFpbCI6InN0cmlkZXIubGVlQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJhdF9oYXNoIjoiR21KVzJaNWh2WTVULTA1UXMwZlRLZyIsIm5hbWUiOiJDaHJpcyBMZWUiLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jSmx4TUoyR1JpOVZuZzJvYk9aTF92cy1jSzhhVzZvdVh3Wmhsc1c2eFQ0c1hrVTdjbDh4QT1zOTYtYyIsImdpdmVuX25hbWUiOiJDaHJpcyIsImZhbWlseV9uYW1lIjoiTGVlIiwiaWF0IjoxNzQzNTc1NjI2LCJleHAiOjE3NDM1NzkyMjZ9.ZhBBS6k9ZDTGqkXJMEbTEEwvxpdNOKXC5byH6uuoiU3oO_TIedL2lm05YdSXHQnG-vbJ9LVc3LFgcmqPT-DQ59i71y0jvCFMQP5DlcfUV0dq7AA1RZv_pwFFGgNqgSpUifzmrrV9VpKr7xMjwhPNSfNRx3EdNogzjKEZPcFfCz777auqPVC8KJgpUp3Pa7GhPRsLdGmH3QACpNaw1ilQx7YPuz6_5tyT86JAvn7LH9F86_1ceju1-ynPEAeFLWgsFe2DFOMonwwUQnx3c-RTJGyKTwZiFwb-ssBWHJGacvCx3Xr29aHhoXb5FCYK3Yf9rpgCrEStmNYoCAWkDjayZQ",
                                              "refreshToken": "ref-token",
                                              "email": "strider@gmail.com",
                                              "password": "123456",
                                              "username": "striderlee",
                                              "role": "USER",
                                              "channelId": "UC_x5XG..."
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully."
            ),
//            @ApiResponse(
//                 responseCode = "404",
//                 description = "API path not found.",
//                 content = @Content(schema = @Schema(implementation = CommonErrorResponse.class))
//            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or expired Google ID token.",
                    content = @Content(schema = @Schema(implementation = RestAPIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Email or Username already exists.",
                    content = @Content(schema = @Schema(implementation = RestAPIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Unexpected server error during registration.",
                    content = @Content(schema = @Schema(implementation = RestAPIResponse.class))
            )
    })
    // This logic will be used when user logs in for the first time from the iOS application
    @PostMapping(value = "/signup")
    public ResponseEntity<MemberRegisterResponse> signup(@Valid @RequestBody MemberRegisterRequest request) {
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/auth/register").toUriString());
        return ResponseEntity.created(uri).body(authService.signup(request));
    }


    // This is method have same function of login()
    //most time when user was logged out because of
    //  1. token expired
    //  2. when iOS was reactive
    @Operation(summary = "login user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),//success
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = RestAPIResponse.class))),//unauthorized  error
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = RestAPIResponse.class))),//not found error
            @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))//internal server error
    })
    @PostMapping(value = "/login")
    public ResponseEntity<?> login(@Valid @RequestBody MemberRegisterRequest request) {

        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/auth/login").toUriString());
        return ResponseEntity.created(uri).body(authService.loginWithIdAndPassword(request));
    }


    @PostMapping(value = "/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/auth/logout").toUriString());
        return ResponseEntity.created(uri).body(authService.logout(request));
    }

    @Operation(summary = "Refresh Token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201"),//created
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = RestAPIResponse.class))),//unauthorized  error
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = RestAPIResponse.class))),//not found error
            @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))//internal server error
    })
    @PostMapping(value = "/refreshToken")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {


        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/auth/refreshToken").toUriString());
        return ResponseEntity.created(uri).body(authService.refreshToken(request));


    }


}
