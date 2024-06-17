package com.adventuretube.security.model;


import org.springframework.security.core.userdetails.UserDetails;

import com.adventuretube.common.domain.dto.auth.AuthDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private AuthDTO  userDetails;
    private String accessToken;
    private String refreshToken;
//    public AuthResponse(String accessToken, String refreshToken) {
//        this.accessToken = accessToken;
//        this.refreshToken = refreshToken;
//    }
}
