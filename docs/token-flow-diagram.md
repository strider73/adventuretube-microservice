# AdventureTube Token Flow Diagram

## Overview
This document describes the complete token lifecycle in the AdventureTube microservices architecture, including registration, login, token refresh, and logout flows.

---

## 1. User Registration Flow

```mermaid
sequenceDiagram
    participant Client
    participant AuthService
    participant GoogleAPI
    participant MemberService
    participant Database

    Client->>AuthService: POST /auth/users<br/>{googleIdToken, email, password}

    AuthService->>AuthService: Decode JWT payload
    AuthService->>AuthService: Verify audience (aud) claim

    AuthService->>GoogleAPI: Verify Google ID Token
    GoogleAPI-->>AuthService: Token Valid ✓

    AuthService->>AuthService: Compare email in request<br/>vs email in Google payload

    AuthService->>MemberService: POST /member/emailDuplicationCheck<br/>{email}
    MemberService->>Database: SELECT * FROM member<br/>WHERE email = ?
    Database-->>MemberService: Query Result
    MemberService-->>AuthService: Email exists: false ✓

    AuthService->>MemberService: POST /member/registerMember<br/>{memberDTO}
    MemberService->>Database: INSERT INTO member
    Database-->>MemberService: Member Created
    MemberService-->>AuthService: MemberDTO {id, email, role}

    AuthService->>AuthService: Generate Access Token (JWT)<br/>Claims: {email, role, type: ACCESS}
    AuthService->>AuthService: Generate Refresh Token (JWT)<br/>Claims: {email, role, type: REFRESH}

    AuthService->>MemberService: POST /member/storeTokens<br/>{memberDTO, accessToken, refreshToken}
    MemberService->>Database: DELETE existing tokens<br/>for member_id
    MemberService->>Database: INSERT INTO token<br/>{member_id, access_token, refresh_token}
    Database-->>MemberService: Token Stored
    MemberService-->>AuthService: Success: true

    AuthService-->>Client: 201 Created<br/>{userId, accessToken, refreshToken}
```

---

## 2. User Login Flow

```mermaid
sequenceDiagram
    participant Client
    participant AuthService
    participant GoogleAPI
    participant CustomUserDetailService
    participant MemberService
    participant Database

    Client->>AuthService: POST /auth/token<br/>{email, password, googleIdToken}

    AuthService->>AuthService: Decode JWT payload
    AuthService->>AuthService: Verify audience claim

    AuthService->>GoogleAPI: Verify Google ID Token
    GoogleAPI-->>AuthService: Token Valid ✓

    AuthService->>AuthService: Extract email & googleId<br/>from Google payload

    AuthService->>AuthService: AuthenticationManager.authenticate()<br/>{email, googleId}

    AuthService->>CustomUserDetailService: loadUserByUsername(email)
    CustomUserDetailService->>MemberService: POST /member/findMemberByEmail<br/>{email}
    MemberService->>Database: SELECT * FROM member<br/>WHERE email = ?
    Database-->>MemberService: Member Data
    MemberService-->>CustomUserDetailService: MemberDTO
    CustomUserDetailService-->>AuthService: UserDetails

    AuthService->>AuthService: Validate credentials<br/>(googleId matches)

    AuthService->>AuthService: Generate Access Token
    AuthService->>AuthService: Generate Refresh Token

    AuthService->>MemberService: POST /member/storeTokens<br/>{memberDTO, accessToken, refreshToken}
    MemberService->>Database: DELETE existing tokens
    MemberService->>Database: INSERT new token
    Database-->>MemberService: Token Stored
    MemberService-->>AuthService: Success: true

    AuthService-->>Client: 200 OK<br/>{accessToken, refreshToken}
```

---

## 3. Token Refresh Flow

```mermaid
sequenceDiagram
    participant Client
    participant AuthService
    participant JwtAuthFilter
    participant MemberService
    participant Database

    Client->>AuthService: POST /auth/token/refresh<br/>Header: Authorization: Bearer {refreshToken}

    AuthService->>JwtAuthFilter: Validate JWT (Gateway level)
    JwtAuthFilter->>JwtAuthFilter: Extract username from token
    JwtAuthFilter->>JwtAuthFilter: Validate token signature
    JwtAuthFilter-->>AuthService: Token Valid ✓

    AuthService->>AuthService: Extract refresh token from header

    AuthService->>MemberService: POST /member/findToken<br/>{refreshToken}
    MemberService->>Database: SELECT * FROM token<br/>WHERE refresh_token = ?
    Database-->>MemberService: Token Found
    MemberService-->>AuthService: Token exists: true ✓

    AuthService->>AuthService: Extract username & role<br/>from refresh token

    AuthService->>AuthService: Generate new Access Token<br/>Claims: {username, role, type: ACCESS}
    AuthService->>AuthService: Generate new Refresh Token<br/>Claims: {username, role, type: REFRESH}

    AuthService->>MemberService: POST /member/storeTokens<br/>{memberDTO, newAccessToken, newRefreshToken}
    MemberService->>Database: DELETE old tokens
    MemberService->>Database: INSERT new tokens
    Database-->>MemberService: Tokens Stored
    MemberService-->>AuthService: Success: true

    AuthService-->>Client: 200 OK<br/>{accessToken, refreshToken}
```

---

## 4. Logout (Token Revocation) Flow

```mermaid
sequenceDiagram
    participant Client
    participant AuthService
    participant JwtAuthFilter
    participant MemberService
    participant Database

    Client->>AuthService: POST /auth/token/revoke<br/>Header: Authorization: Bearer {accessToken}

    AuthService->>JwtAuthFilter: Validate JWT (Gateway level)
    JwtAuthFilter->>JwtAuthFilter: Extract username
    JwtAuthFilter->>JwtAuthFilter: Validate token
    JwtAuthFilter-->>AuthService: Token Valid ✓

    AuthService->>AuthService: Extract access token from header

    AuthService->>MemberService: POST /member/deleteAllToken<br/>{accessToken}
    MemberService->>Database: DELETE FROM token<br/>WHERE access_token = ?
    Database-->>MemberService: Tokens Deleted (count)
    MemberService-->>AuthService: Success: true

    AuthService-->>Client: 200 OK<br/>{success: true, message: "Logout successful"}
```

---

## 5. JWT Authentication Filter Flow (Gateway/Service Level)

```mermaid
flowchart TD
    A[Incoming Request] --> B{Is path in OPEN_ENDPOINTS?}
    B -->|Yes| C[Skip JWT validation]
    C --> D[Continue to next filter]

    B -->|No| E{Has Authorization header?}
    E -->|No| F[Continue without authentication]

    E -->|Yes| G[Extract token from header]
    G --> H[Extract username from token]

    H --> I{Username exists AND<br/>not authenticated?}
    I -->|No| F

    I -->|Yes| J[Load UserDetails from DB]
    J --> K{Validate token signature<br/>and expiration?}

    K -->|Invalid| F
    K -->|Valid| L[Create Authentication Token]
    L --> M[Set SecurityContext]
    M --> D

    D --> N[Proceed to Controller]
```

---

## Token Storage Strategy

### Database Schema (token table)
```
token {
  id: Long (PK)
  member_id: Long (FK)
  access_token: String
  refresh_token: String
  expired: Boolean
  revoked: Boolean
  created_at: Timestamp
}
```

### Token Management Rules

1. **One Active Token Per User**: When storing a new token, all existing valid tokens for that member are deleted
2. **Token Lifecycle**:
   - Registration → New tokens created
   - Login → Old tokens deleted, new tokens created
   - Refresh → Old tokens deleted, new tokens created
   - Logout → All tokens deleted
3. **Validation**: Tokens are validated at multiple levels:
   - Gateway: JWT signature and expiration
   - Service: Database existence check
   - Google: ID token verification (registration/login only)

---

## Security Flow Summary

| Flow | Google ID Token | JWT Access Token | JWT Refresh Token | DB Token Check |
|------|----------------|------------------|-------------------|----------------|
| **Registration** | ✓ Required | Generated | Generated | Stored |
| **Login** | ✓ Required | Generated | Generated | Stored |
| **Refresh** | ✗ Not used | Generated | ✓ Required | Verified & Replaced |
| **Logout** | ✗ Not used | ✓ Required | ✗ Not used | Deleted |
| **Protected Endpoints** | ✗ Not used | ✓ Required | ✗ Not used | Optional |

---

## Error Handling

### Registration Errors
- `401`: Invalid Google ID Token
- `409`: Email already exists (duplicate)
- `500`: Token save failed / Member service error

### Login Errors
- `401`: Invalid credentials or Google token
- `404`: User not found
- `500`: Authentication error

### Refresh Errors
- `401`: Invalid or expired refresh token
- `404`: Token not found in database
- `500`: Token save failed

### Logout Errors
- `401`: Invalid access token
- `500`: Token deletion failed

---

## Open Endpoints (No JWT Required)

The following endpoints skip JWT validation:
- `/auth/users` (Registration)
- `/auth/token` (Login)
- `/actuator/health` (Health check)
- Other paths defined in `OPEN_ENDPOINTS` constant

All other endpoints require valid JWT access token in Authorization header.