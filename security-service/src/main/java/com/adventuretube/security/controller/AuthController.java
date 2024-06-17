package com.adventuretube.security.controller;


import com.adventuretube.common.error.RestAPIErrorResponse;
import com.adventuretube.security.exceptions.UserNotFoundException;
import com.adventuretube.security.model.AuthRequest;
import com.adventuretube.security.model.AuthResponse;
import com.adventuretube.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final AuthenticationManager authenticationManager;



    @Operation(summary = "Signup user")
    @ApiResponse(responseCode = "201")//created
    //@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = CommonErrorResponse.class)))
    @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = RestAPIErrorResponse.class)))//unauthorized  error
    @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = RestAPIErrorResponse.class)))//conflict error
    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = RestAPIErrorResponse.class)))//internal server error
    //This logic will be used when user login first time from the ios application
    @PostMapping(value = "/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
               URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/auth/register").toUriString());
        return ResponseEntity.created(uri).body(authService.register(request));
    }


    // This is method have same function of login()
    //most time when user was logged out because of
    //  1. token expired
    //  2. when iOS was reactive
    @Operation(summary = "SignIn user")
    @ApiResponse(responseCode = "200")//success
    @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = RestAPIErrorResponse.class)))//unauthorized  error
    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = RestAPIErrorResponse.class)))//not found error
    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = RestAPIErrorResponse.class)))//internal server error
    @PostMapping(value = "/getToken")
    public ResponseEntity<?> getToken(@Valid @RequestBody AuthRequest request) {

            // Authenticate the user

            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/auth/getToken").toUriString());

            return ResponseEntity.created(uri).body(authService.getToken(userDetails));
    }

}
