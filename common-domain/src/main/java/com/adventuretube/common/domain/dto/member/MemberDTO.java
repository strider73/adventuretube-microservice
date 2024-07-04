package com.adventuretube.common.domain.dto.member;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberDTO {
    private UUID id;

    private String email;
    private String password;
    private String username;

    private String googleIdToken;

    private Long googleIdTokenExp;

    private Long googleIdTokenIat;

    //A unique string value used to associate a client and mitigate replay attacks
    private String googleIdTokenSub;

    private String googleProfilePicture;

    private String channelId;


    private String role;

}
