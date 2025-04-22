## 🔐 Spring Security Authentication Workflow in AdventureTube Auth-Service

This document outlines the internal security components and authentication flow of the `auth-service` module in the AdventureTube microservices system. It demonstrates how Spring Security is applied to support authentication, JWT handling, and secure login flows.

---

### 🧭 Overview

The `auth-service` is responsible for:
- Authenticating users with credentials (email/password)
- Issuing JWT access and refresh tokens
- Validating and parsing JWT tokens
- Providing secure access to `/auth/**` endpoints

Security in this module is implemented using **Spring Security** with a fully **servlet-based architecture**. JWT tokens are handled via a custom filter, and user identity is authenticated against a member service.

---

### 🧩 Core Security Classes

#### 1. `AuthServiceConfig`
- Central Spring Security configuration class
- Applies security only to `/auth/**` routes
- Defines public endpoints (`/auth/login`, `/auth/register`, `/auth/refreshToken`, etc.)
- Requires `ADMIN` role for all other secured endpoints
- Injects `JwtAuthFilter` to validate tokens before default Spring filters
- Configures `AuthenticationManager` using `CustomUserDetailService`

#### 2. `CustomUserDetailService`
- Implements `UserDetailsService`
- Uses `RestTemplate` to call `MEMBER-SERVICE` for user details by email
- Validates user existence and loads their roles and encrypted password
- Used during login to verify credentials

#### 3. `JwtUtil`
- Manages JWT creation and validation
- Supports:
    - `generate()` — creates signed tokens with expiration and roles
    - `getClaims()` — parses and verifies token signature and expiry
    - `validateToken()` — confirms subject identity
- Uses `@PostConstruct` to prepare the signing key from secret value

#### 4. `CustomAuthenticationProvider` *(optional/custom)*
- Extends `DaoAuthenticationProvider`
- Allows deeper control over credential validation logic
- Currently commented out, but ready for activation if needed

---

### 🔄 Full Authentication Flow Diagram

This is the standard Spring Security authentication chain, adapted to your architecture:

```
User enters credentials
        ↓
Authentication Filter
        ↓
Authentication Manager
        ↓
Authentication Provider
      ↙       ↘
UserDetailsService   PasswordEncoder
        ↓
Security Context
```

---

### 📦 How This Maps to Your Classes

| Step | Component                  | Description                                                                                   | Your Implementation                                     |
|------|----------------------------|-----------------------------------------------------------------------------------------------|----------------------------------------------------------|
| 1    | User Credentials           | User submits `/auth/login` with email + password                                              | REST Controller or default filter                       |
| 2    | Authentication Filter      | Creates token and passes to `AuthenticationManager`                                           | Custom or default Spring Security filter                |
| 3    | Authentication Manager     | Delegates auth logic to provider                                                              | `customAuthenticationManager()` in `AuthServiceConfig`  |
| 4    | Authentication Provider    | Validates credentials using user service and password encoder                                 | `CustomAuthenticationProvider` *(optional)*             |
| 5    | UserDetailsService         | Loads user by email, returns `UserDetails` object                                             | `CustomUserDetailService`                               |
| 6    | PasswordEncoder            | Compares raw password with encoded password                                                   | `BCryptPasswordEncoder`                                 |
| 7    | Authentication             | If valid, returns an authenticated token                                                      | Managed internally by Spring                            |
| 8    | SecurityContext            | Stores authentication so it can be accessed throughout the request lifecycle                  | Automatically handled by Spring                         |
| 9    | JWT Generation (Post Auth) | Issue signed JWT with role, id, expiration                                                    | `JwtUtil.generate()`                                    |

---

### ✅ Summary

The authentication workflow in your `auth-service` is structured around standard Spring Security components, extended with:

- A custom `UserDetailsService` fetching users from `MEMBER-SERVICE`
- A `PasswordEncoder` for credential verification
- Optional `AuthenticationProvider` for custom credential handling
- A `JwtUtil` for secure token issuance and claim validation

This setup ensures a clean, extensible, and secure architecture for handling user authentication and issuing JWT tokens.

