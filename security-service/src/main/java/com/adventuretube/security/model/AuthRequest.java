package com.adventuretube.security.model;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthRequest {


    private String googleIdToken;
    private String email;
    private String password;
    private String username;

    private String role;
    private String channelId;
}
