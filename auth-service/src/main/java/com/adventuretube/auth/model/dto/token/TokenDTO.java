package com.adventuretube.auth.model.dto.token;

import com.adventuretube.auth.model.dto.member.MemberDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenDTO {

    public UUID id;
    public String accessToken;
    public String refreshToken;

    public boolean revoked;

    public boolean expired;
    public MemberDTO memberDTO;
}
