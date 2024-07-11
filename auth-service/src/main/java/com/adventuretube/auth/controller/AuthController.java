package com.adventuretube.auth.controller;


import com.adventuretube.common.error.RestAPIResponse;
import com.adventuretube.auth.model.MemberRegisterRequest;
import com.adventuretube.auth.model.MemberRegisterResponse;
import com.adventuretube.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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



    @Operation(summary = "Signup user")
    @ApiResponse(responseCode = "201")//created
    //@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = CommonErrorResponse.class)))
    @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))//unauthorized  error
    @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))//conflict error
    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))//internal server error
    // This logic will be used when user login first time from the ios application
    @PostMapping(value = "/register")
    public ResponseEntity<MemberRegisterResponse> register(@Valid @RequestBody MemberRegisterRequest request) {
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/auth/register").toUriString());
        return ResponseEntity.created(uri).body(authService.register(request));
    }


    // This is method have same function of login()
    //most time when user was logged out because of
    //  1. token expired
    //  2. when iOS was reactive
    @Operation(summary = "login user")
    @ApiResponse(responseCode = "200")//success
    @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))//unauthorized  error
    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))//not found error
    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))//internal server error
    @PostMapping(value = "/login")
    public ResponseEntity<?> login(@Valid @RequestBody MemberRegisterRequest request) {

        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/auth/login").toUriString());
        return ResponseEntity.created(uri).body(authService.loginWithIdAndPassword(request));
    }


    @PostMapping(value = "/logout")
    public ResponseEntity<?> logout(HttpServletRequest  request){
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/auth/logout").toUriString());
        return ResponseEntity.created(uri).body(authService.logout(request));
    }

    @Operation(summary = "Refresh Token")
    @ApiResponse(responseCode = "201")//created
    @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))//unauthorized  error
    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))//not found error
    @ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = RestAPIResponse.class)))//internal server error
    @PostMapping(value = "/refreshToken")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {


        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/auth/refreshToken").toUriString());
        return ResponseEntity.created(uri).body(authService.refreshToken(request));


    }


}
