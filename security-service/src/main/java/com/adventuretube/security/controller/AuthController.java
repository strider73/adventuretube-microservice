package com.adventuretube.security.controller;


import com.adventuretube.security.model.AuthRequest;
import com.adventuretube.security.model.AuthResponse;
import com.adventuretube.security.service.AuthService;
import com.adventuretube.security.service.JwtUtil;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    //This logic will be used when user login first time from the ios application
    @PostMapping(value = "/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request){
        return ResponseEntity.ok(authService.register(request));
    }


    // This is method have same function of login()
    //most time when user was logged out because of
    //  1. token expired
    //  2. when iOS was reactive
    @PostMapping(value = "/getToken")
    public ResponseEntity<AuthResponse> getToken(@Valid @RequestBody AuthRequest request){


        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            return ResponseEntity.ok(authService.getToken(userDetails));

        }catch (BadCredentialsException e){
            //TODO add login Attempt
            throw  e;
        }


    }

}
