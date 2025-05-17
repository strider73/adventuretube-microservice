package com.adventuretube.auth.model.response;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberRegisterResponse {
    //private MemberDTO userDetails;
    private UUID userId;
    private String accessToken;
    private String refreshToken;
//    public AuthResponse(String accessToken, String refreshToken) {
//        this.accessToken = accessToken;
//        this.refreshToken = refreshToken;
//    }
}
