package com.adventuretube.auth.model;


import com.adventuretube.common.domain.dto.auth.MemberDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MemberRegisterResponse {
    private MemberDTO userDetails;
    private String accessToken;
    private String refreshToken;
//    public AuthResponse(String accessToken, String refreshToken) {
//        this.accessToken = accessToken;
//        this.refreshToken = refreshToken;
//    }
}
