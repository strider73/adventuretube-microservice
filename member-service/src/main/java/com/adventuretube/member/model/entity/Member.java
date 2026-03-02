package com.adventuretube.member.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "google_id_token")
    private String googleIdToken;

    @Column(name = "google_id_token_exp")
    private Long googleIdTokenExp;

    @Column(name = "google_id_token_iat")
    private Long googleIdTokenIat;

    @Column(name = "google_id_token_sub")
    private String googleIdTokenSub;

    @Column(name = "google_profile_picture")
    private String googleProfilePicture;

    private String username;
    private String password;

    @Column(name = "channel_id")
    private String channelId;

    private String email;
    private String role;

    @Column(name = "create_at")
    private LocalDateTime createAt;

    @PrePersist
    protected void onCreate() {
        if (createAt == null) {
            createAt = LocalDateTime.now();
        }
    }
}
