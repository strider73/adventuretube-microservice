package com.adventuretube.security.model;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthRequest {


    @Schema(description = "Google ID Token", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6Ij...")
    @NotBlank(message = "Google ID Token cannot be blank")
    private String googleIdToken;

    @Schema(description = "email", example = "strider@gmail.com")
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "password", example = "123456")
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters")
    private String password;


    @Schema(description = "username", example = "striderlee")
    @NotBlank(message = "username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Schema(description = "Role", example = "USER")
    @NotBlank(message = "Role cannot be blank")
    private String role;


    @Schema(description = "Channel ID", example = "UC_x5XG1OV2P6uZZ5FSM9Ttw")
    private String channelId;
}
