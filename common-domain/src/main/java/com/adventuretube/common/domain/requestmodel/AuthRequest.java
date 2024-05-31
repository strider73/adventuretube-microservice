package com.adventuretube.common.domain.requestmodel;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthRequest {
    private String email;
    private String password;
    private String username;

    private String channelID;
}
