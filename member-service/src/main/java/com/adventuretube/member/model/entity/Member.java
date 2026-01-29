package com.adventuretube.member.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
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
 * - Persistable: Implemented to control INSERT vs UPDATE behavior. Spring Data R2DBC
 *   uses isNew() to determine whether to INSERT or UPDATE. Without this, setting
 *   id before save causes R2DBC to attempt UPDATE (fails with "Row does not exist").
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
public class Member implements Persistable<UUID> {

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

    /**
     * Transient flag to indicate if this is a new entity.
     * Used by Persistable.isNew() to control INSERT vs UPDATE behavior.
     * Must be set to true before saving a new entity.
     */
    @Transient
    @Builder.Default
    private boolean isNew = false;

    @Override
    public boolean isNew() {
        return isNew;
    }

    /**
     * Mark this entity as new (for INSERT) or existing (for UPDATE).
     * Call setNew(true) before saving a new member.
     */
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
}
