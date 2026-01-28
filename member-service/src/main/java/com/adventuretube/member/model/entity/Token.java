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
 * Token entity for R2DBC.
 *
 * Note: R2DBC does not support @ManyToOne relationships.
 * Instead of `Member member`, we store `memberId` (the foreign key).
 *
 * Implements Persistable to control INSERT vs UPDATE behavior.
 * R2DBC uses isNew() to decide: true = INSERT, false = UPDATE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("token")
public class Token implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Column("access_token")
    private String accessToken;

    @Column("refresh_token")
    private String refreshToken;

    private boolean revoked;
    private boolean expired;

    @Column("create_at")
    private LocalDateTime createAt;

    // R2DBC: Store foreign key instead of object reference
    @Column("member_id")
    private UUID memberId;

    @Override
    public boolean isNew() {
        return isNew;
    }
}
