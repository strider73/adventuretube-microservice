package com.adventuretube.member.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Member entity for R2DBC.
 *
 * R2DBC Limitations & Workarounds:
 * - @PrePersist: Not supported. Workaround in MemberService.registerMember()
 *   sets createAt = LocalDateTime.now() before save.
 * - @GeneratedValue: Not supported. Workaround in MemberService.registerMember()
 *   sets id = UUID.randomUUID() before save.
 * - UserDetails: Removed from entity. Before (JPA), Member did double duty
 *   (data persistence + security). After (R2DBC), clean separation -
 *   Member handles only data persistence, security handled in auth-service.
 *
 * @see MemberService#registerMember(Member)
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("member")
public class Member {

    @Id
    private UUID id;

    @Column("google_id_token")
    private String googleIdToken;

    @Column("google_id_token_exp")
    private Long googleIdTokenExp;

    @Column("google_id_token_iat")
    private Long googleIdTokenIat;

    @Column("google_id_token_sub")
    private String googleIdTokenSub;

    @Column("google_profile_picture")
    private String googleProfilePicture;

    private String username;
    private String password;

    @Column("channel_id")
    private String channelId;

    private String email;
    private String role;

    @Column("create_at")
    private LocalDateTime createAt;
}
