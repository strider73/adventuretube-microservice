package com.adventuretube.auth.controller;


import com.adventuretube.auth.exceptions.code.AuthErrorCode;
import com.adventuretube.auth.model.request.MemberLoginRequest;
import com.adventuretube.auth.model.response.MemberLoginResponse;
import com.adventuretube.auth.model.request.MemberRegisterRequest;
import com.adventuretube.auth.model.response.MemberRegisterResponse;
import com.adventuretube.auth.service.AuthService;
import com.adventuretube.common.api.response.ServiceResponse;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;

    // =========================
    // Registration Endpoint
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
                                              "googleIdToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjgyMWYzYmM2NmYwNzUxZjc4NDA2MDY3OTliMWFkZjllOWZiNjBkZmIiLCJ0eXAiOiJKV1QifQ...",
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
                    description = "User registered successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": null,
                                      "errorCode": null,
                                      "data": {
                                        "userId": "550e8400-e29b-41d4-a716-446655440000",
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                                      },
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed - Invalid request body.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Validation failed",
                                      "errorCode": "VALIDATION_FAILED",
                                      "data": {
                                        "email": "Invalid email format",
                                        "password": "Password must be between 6 and 20 characters"
                                      },
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid Google ID token, email mismatch, or duplicate email.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Google ID token is invalid",
                                      "errorCode": "GOOGLE_TOKEN_INVALID",
                                      "data": null,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Internal server error",
                                      "errorCode": "INTERNAL_ERROR",
                                      "data": null,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service Unavailable - Member service unreachable.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Member service is unavailable",
                                      "errorCode": "SERVICE_UNAVAILABLE",
                                      "data": null,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            )
    })
    // This logic will be used when user logs in for the first time from the iOS application
    @PostMapping(value = "/users")
    public Mono<ResponseEntity<?>> registerUser(@Valid @RequestBody MemberRegisterRequest request) {
        return authService.createUser(request)
                .map(response -> ResponseEntity.created(
                        UriComponentsBuilder.fromPath("/users/{id}")
                                .buildAndExpand(response.getData().getUserId())
                                .toUri()
                ).body(response));
    }

    // =========================
    // Login Endpoint
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
                                              "email": "strider.lee@gmail.com",
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
                    description = "Tokens issued successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": null,
                                      "errorCode": null,
                                      "data": {
                                        "userId": null,
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                                      },
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed - Invalid request body.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Validation failed",
                                      "errorCode": "VALIDATION_FAILED",
                                      "data": {
                                        "email": "Invalid email format",
                                        "password": "Password must be between 6 and 20 characters"
                                      },
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid credentials, expired Google ID token, or user not found.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Google ID token is invalid",
                                      "errorCode": "GOOGLE_TOKEN_INVALID",
                                      "data": null,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Internal server error",
                                      "errorCode": "INTERNAL_ERROR",
                                      "data": null,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service Unavailable - Member service unreachable.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Member service is unavailable",
                                      "errorCode": "SERVICE_UNAVAILABLE",
                                      "data": null,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            )
    })
    @PostMapping(value = "/token")
    public Mono<ResponseEntity<?>> issueToken(@Valid @RequestBody MemberLoginRequest request) {
        return authService.issueToken(request)
                .map(response -> ResponseEntity.ok(response));
    }

    // =========================
    // Refresh Token Endpoint
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
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": null,
                                      "errorCode": null,
                                      "data": {
                                        "userId": null,
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                                      },
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Refresh token is missing, invalid, expired, or not found.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Token not found in the database",
                                      "errorCode": "TOKEN_NOT_FOUND",
                                      "data": null,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Internal server error",
                                      "errorCode": "INTERNAL_ERROR",
                                      "data": null,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service Unavailable - Member service unreachable.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Member service is unavailable",
                                      "errorCode": "SERVICE_UNAVAILABLE",
                                      "data": null,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            )
    })
    @PostMapping(value = "/token/refresh")
    public Mono<ResponseEntity<?>> refreshToken(@RequestHeader("Authorization") String authorization) {
        return authService.refreshToken(authorization)
                .map(response -> ResponseEntity.ok(response));
    }


    // =========================
    // Logout Endpoint
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
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Logout has been successful",
                                      "errorCode": null,
                                      "data": true,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing refresh token.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Token deletion failed",
                                      "errorCode": "TOKEN_DELETION_FAILED",
                                      "data": null,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - Token revocation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Internal server error",
                                      "errorCode": "INTERNAL_ERROR",
                                      "data": null,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            )
    })
    @PostMapping(value = "/token/revoke")
    public Mono<ResponseEntity<?>> revokeToken(@RequestHeader("Authorization") String authorization) {
        return authService.revokeToken(authorization)
                .map(response -> ResponseEntity.ok(response));
    }

    // =========================
    // Delete User Endpoint
    // =========================
    @Operation(
            summary = "Delete user and all associated tokens",
            description = """
                    This endpoint deletes a user and all their tokens from the system.
                    It calls the MEMBER-SERVICE to perform the deletion.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "text/plain",
                            examples = @ExampleObject(
                                    name = "Delete User Example",
                                    value = "strider.lee@gmail.com"
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User deleted successfully.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "User deleted successfully",
                                      "errorCode": null,
                                      "data": true,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error - User deletion failed.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Member deletion failed",
                                      "errorCode": "MEMBER_DELETION_FAILED",
                                      "data": null,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service Unavailable - Member service unreachable.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ServiceResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "message": "Member service is unavailable",
                                      "errorCode": "SERVICE_UNAVAILABLE",
                                      "data": null,
                                      "timestamp": "2026-03-22T14:00:00"
                                    }
                                    """)
                    )
            )
    })
    @DeleteMapping(value = "/users")
    public Mono<ResponseEntity<?>> deleteUser(@RequestBody String email) {
        return authService.deleteUser(email)
                .map(response -> ResponseEntity.ok(response));
    }


}
