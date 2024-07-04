package com.adventuretube.common.domain.dto.token;


import com.adventuretube.common.domain.dto.member.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Token {

    @Id
    @GeneratedValue
    public UUID id;

    @Column(unique = true)
    public String accessToken;
    @Column(unique = true)
    public String refreshToken;
    public boolean revoked;

    public boolean expired;

//    public Long member_id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    public Member member;
}