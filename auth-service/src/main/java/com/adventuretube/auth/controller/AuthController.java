package com.adventuretube.auth.controller;


import com.adventuretube.auth.model.request.MemberLoginRequest;
import com.adventuretube.auth.model.response.MemberLoginResponse;
import com.adventuretube.auth.common.response.RestAPIResponse;
import com.adventuretube.auth.model.request.MemberRegisterRequest;
import com.adventuretube.auth.model.response.MemberRegisterResponse;
import com.adventuretube.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

    // =========================
    // 🔐 Registration Endpoint
    // =========================
    @Operation(
            summary = "Create  new user and issue id and refresh token",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Signup Example",
                                    value = """
                                            {
                                              "googleIdToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjgyMWYzYmM2NmYwNzUxZjc4NDA2MDY3OTliMWFkZjllOWZiNjBkZmIiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI2NTc0MzMzMjMzMzctN2dlMzc1ODBsZGtqczNpMTNycW4ycGMydmFmNjFrcGQuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI2NTc0MzMzMjMzMzctN2dlMzc1ODBsZGtqczNpMTNycW4ycGMydmFmNjFrcGQuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMTA4MTQ5NzI0OTUwMjgwOTM1NDkiLCJlbWFpbCI6InN0cmlkZXIubGVlQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJhdF9oYXNoIjoiR21KVzJaNWh2WTVULTA1UXMwZlRLZyIsIm5hbWUiOiJDaHJpcyBMZWUiLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jSmx4TUoyR1JpOVZuZzJvYk9aTF92cy1jSzhhVzZvdVh3Wmhsc1c2eFQ0c1hrVTdjbDh4QT1zOTYtYyIsImdpdmVuX25hbWUiOiJDaHJpcyIsImZhbWlseV9uYW1lIjoiTGVlIiwiaWF0IjoxNzQzNTc1NjI2LCJleHAiOjE3NDM1NzkyMjZ9.ZhBBS6k9ZDTGqkXJMEbTEEwvxpdNOKXC5byH6uuoiU3oO_TIedL2lm05YdSXHQnG-vbJ9LVc3LFgcmqPT-DQ59i71y0jvCFMQP5DlcfUV0dq7AA1RZv_pwFFGgNqgSpUifzmrrV9VpKr7xMjwhPNSfNRx3EdNogzjKEZPcFfCz777auqPVC8KJgpUp3Pa7GhPRsLdGmH3QACpNaw1ilQx7YPuz6_5tyT86JAvn7LH9F86_1ceju1-ynPEAeFLWgsFe2DFOMonwwUQnx3c-RTJGyKTwZiFwb-ssBWHJGacvCx3Xr29aHhoXb5FCYK3Yf9rpgCrEStmNYoCAWkDjayZQ",
                                              "refreshToken": "ref-token",
                                              "email": "strider.lee@gmail.com",
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
    @PostMapping(value = "/users")
    public ResponseEntity<MemberRegisterResponse> registerUser(@Valid @RequestBody MemberRegisterRequest request) {
        MemberRegisterResponse response = authService.createUser(request); // 🔥 renamed here too
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/users/{id}")
                .buildAndExpand(response.getUserId())
                .toUriString());
        return ResponseEntity.created(uri).body(response);
    }

    // =========================
    // 🔑 Login Endpoint
    // =========================
    @Operation(
            summary = "Authenticate user and issue access and refresh tokens",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Login Example",
                                    value = """
                                            {
                                              "email": "strider@gmail.com",
                                              "password": "123456",
                                              "googleIdToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6I..."
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tokens issued successfully."
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid credentials or expired Google ID token.",
                    content = @Content(schema = @Schema(implementation = RestAPIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found.",
                    content = @Content(schema = @Schema(implementation = RestAPIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Unexpected authentication error.",
                    content = @Content(schema = @Schema(implementation = RestAPIResponse.class))
            )
    })
    @PostMapping(value = "/token")
    public ResponseEntity<?> issueToken(@Valid @RequestBody MemberLoginRequest request) {

        return ResponseEntity.ok(authService.issueToken(request));
    }

    // =========================
    // 🔄 Refresh Token Endpoint
    // =========================
    @Operation(
            summary = "Refresh access token using a valid refresh token",
            description = """
                         This endpoint accepts a valid refresh token via the Authorization header.
                         It verifies the token against the token store in the MEMBER-SERVICE.
                         If valid, it issues a new access token and a new refresh token.
                            
                          This should be called when an access token is expired but the refresh token is still active.
                    """,
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            description = "Refresh token in the format: Bearer {refresh_token}",
                            required = true,
                            in = ParameterIn.HEADER,
                            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Access token refreshed successfully.",
                    content = @Content(schema = @Schema(implementation = MemberLoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Refresh token is missing, invalid, or expired.",
                    content = @Content(schema = @Schema(implementation = RestAPIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found.",
                    content = @Content(schema = @Schema(implementation = RestAPIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Unexpected failure during token refresh.",
                    content = @Content(schema = @Schema(implementation = RestAPIResponse.class))
            )
    })
    @PostMapping(value = "/refreshToken")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }


    // =========================
    // 🚪 Logout Endpoint
    // =========================
    @Operation(
            summary = "Logout user and revoke refresh token",
            description = """
                    This endpoint handles user logout.

                    It expects the refresh token in the `Authorization` header and performs:
                    - validation of the token
                    - deletion of the refresh token from the MEMBER-SERVICE
                    - revocation of all active tokens for the user

                    The request is authenticated via JWT by the API Gateway before reaching this method.
                    """,
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            description = "Refresh token in the format: Bearer {refresh_token}",
                            required = true,
                            in = ParameterIn.HEADER,
                            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User successfully logged out. Refresh token deleted.",
                    content = @Content(schema = @Schema(implementation = RestAPIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing refresh token.",
                    content = @Content(schema = @Schema(implementation = RestAPIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Token revocation failed.",
                    content = @Content(schema = @Schema(implementation = RestAPIResponse.class))
            )
    })
    @PostMapping(value = "/logout")
    public ResponseEntity<?> revokeRefreshToken(HttpServletRequest request) {
        return ResponseEntity.ok(authService.logout(request));
    }
}
