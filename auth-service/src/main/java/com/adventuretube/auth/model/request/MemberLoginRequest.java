package com.adventuretube.auth.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MemberLoginRequest {
    @Schema(description = "email", example = "strider@gmail.com")
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")    private String email;

    @Schema(description = "password", example = "123456")
    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters")
    private String password;

    @Schema(description = "Google ID Token", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjgyMWYzYmM2NmYwNzUxZjc4NDA2MDY3OTliMWFkZjllOWZiNjBkZmIiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI2NTc0MzMzMjMzMzctN2dlMzc1ODBsZGtqczNpMTNycW4ycGMydmFmNjFrcGQuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI2NTc0MzMzMjMzMzctN2dlMzc1ODBsZGtqczNpMTNycW4ycGMydmFmNjFrcGQuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMTA4MTQ5NzI0OTUwMjgwOTM1NDkiLCJlbWFpbCI6InN0cmlkZXIubGVlQGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJhdF9oYXNoIjoiR21KVzJaNWh2WTVULTA1UXMwZlRLZyIsIm5hbWUiOiJDaHJpcyBMZWUiLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDMuZ29vZ2xldXNlcmNvbnRlbnQuY29tL2EvQUNnOG9jSmx4TUoyR1JpOVZuZzJvYk9aTF92cy1jSzhhVzZvdVh3Wmhsc1c2eFQ0c1hrVTdjbDh4QT1zOTYtYyIsImdpdmVuX25hbWUiOiJDaHJpcyIsImZhbWlseV9uYW1lIjoiTGVlIiwiaWF0IjoxNzQzNTc1NjI2LCJleHAiOjE3NDM1NzkyMjZ9.ZhBBS6k9ZDTGqkXJMEbTEEwvxpdNOKXC5byH6uuoiU3oO_TIedL2lm05YdSXHQnG-vbJ9LVc3LFgcmqPT-DQ59i71y0jvCFMQP5DlcfUV0dq7AA1RZv_pwFFGgNqgSpUifzmrrV9VpKr7xMjwhPNSfNRx3EdNogzjKEZPcFfCz777auqPVC8KJgpUp3Pa7GhPRsLdGmH3QACpNaw1ilQx7YPuz6_5tyT86JAvn7LH9F86_1ceju1-ynPEAeFLWgsFe2DFOMonwwUQnx3c-RTJGyKTwZiFwb-ssBWHJGacvCx3Xr29aHhoXb5FCYK3Yf9rpgCrEStmNYoCAWkDjayZQ")
    private String googleIdToken;
}
