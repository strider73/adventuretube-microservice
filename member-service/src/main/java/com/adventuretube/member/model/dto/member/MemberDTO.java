package com.adventuretube.member.model.dto.member;


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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getGoogleProfilePicture() {
        return googleProfilePicture;
    }

    public void setGoogleProfilePicture(String googleProfilePicture) {
        this.googleProfilePicture = googleProfilePicture;
    }

    public String getGoogleIdTokenSub() {
        return googleIdTokenSub;
    }

    public void setGoogleIdTokenSub(String googleIdTokenSub) {
        this.googleIdTokenSub = googleIdTokenSub;
    }

    public Long getGoogleIdTokenIat() {
        return googleIdTokenIat;
    }

    public void setGoogleIdTokenIat(Long googleIdTokenIat) {
        this.googleIdTokenIat = googleIdTokenIat;
    }

    public Long getGoogleIdTokenExp() {
        return googleIdTokenExp;
    }

    public void setGoogleIdTokenExp(Long googleIdTokenExp) {
        this.googleIdTokenExp = googleIdTokenExp;
    }

    public String getGoogleIdToken() {
        return googleIdToken;
    }

    public void setGoogleIdToken(String googleIdToken) {
        this.googleIdToken = googleIdToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }


}
