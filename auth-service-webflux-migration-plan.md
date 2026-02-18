# Auth-Service: Full WebFlux Migration Plan

## Context

Auth-service currently runs on Spring MVC (servlet stack). We're migrating to fully reactive WebFlux with functional RouterFunction endpoints. The migration is broken into 5 sequential steps, each independently committable.

## Dependency Chain

```
Step 0: ServiceClient reactive (prerequisite for everything)
    ↓
Step 1: Security (needs postReactive for CustomUserDetailService)
    ↓
Step 2: Controller-Service (needs reactive AuthService + reactive security)
    ↓
Step 3: Exception handling (needs functional endpoints to exist)
    ↓
Step 4: Swagger (finishing touch)
```

---

## Step 0: ServiceClient Reactive + pom.xml

### ServiceClient.java (MODIFY)
**Path**: `common-api/src/main/java/com/adventuretube/common/client/ServiceClient.java`

- Add `postReactive()` method — same WebClient chain as `post()` but returns `Mono<ServiceResponse<T>>` instead of calling `.block()`
- Add `getReactive()` method — same as `get()` but returns `Mono<ServiceResponse<T>>`
- Keep existing blocking `post()` and `get()` unchanged (other services still use them)

### auth-service/pom.xml (MODIFY)
**Path**: `auth-service/pom.xml`

| Remove | Add |
|--------|-----|
| `spring-boot-starter-web` | (already has `spring-boot-starter-webflux`) |
| `spring-boot-starter-data-jpa` | — |
| `postgresql` | — |

> Note: auth-service has no database. JPA + PostgreSQL were unnecessary dependencies.
> Note: `springdoc-openapi-starter-webmvc-ui` removal moves to Step 4 (Swagger).

### Unit Test: `ServiceClientReactiveTest.java`
**Path**: `common-api/src/test/java/com/adventuretube/common/client/ServiceClientReactiveTest.java`

- Use `MockWebServer` (from `okhttp3`) or `WireMock` to simulate member-service responses
- Test `postReactive()` returns `Mono<ServiceResponse<T>>` for 200, 4xx, 5xx scenarios
- Verify no `.block()` call — test with `StepVerifier`

### Verification
- `mvn compile -pl common-api` — no errors
- `mvn test -pl common-api` — ServiceClientReactiveTest passes
- `mvn compile -pl auth-service -am` — confirms no servlet dependency remains

---

## Step 1: Security (Servlet → WebFlux)

### AuthServiceConfig.java (MODIFY)
**Path**: `auth-service/src/main/java/com/adventuretube/auth/config/security/AuthServiceConfig.java`

| Before (Servlet) | After (WebFlux) |
|---|---|
| `@EnableWebSecurity` | `@EnableWebFluxSecurity` |
| `SecurityFilterChain` bean | `SecurityWebFilterChain` bean |
| `HttpSecurity` parameter | `ServerHttpSecurity` parameter |
| `.securityMatcher("/auth/**")` | `.securityMatcher(new PathPatternParserServerWebExchangeMatcher("/auth/**"))` |
| `.requestMatchers(OPEN_ENDPOINTS).permitAll()` | `.pathMatchers(OPEN_ENDPOINTS).permitAll()` |
| `.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)` | `.addFilterBefore(jwtWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)` |
| `AuthenticationManager` bean (via `HttpSecurity`) | `ReactiveAuthenticationManager` bean |
| `CustomAuthenticationProvider` bean | Logic absorbed into `ReactiveAuthenticationManager` |

### JwtWebFilter.java (NEW — replaces JwtAuthFilter.java)
**Path**: `auth-service/src/main/java/com/adventuretube/auth/filter/JwtWebFilter.java`

| Before (`JwtAuthFilter`) | After (`JwtWebFilter`) |
|---|---|
| `extends OncePerRequestFilter` | `implements WebFilter` |
| `doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)` | `filter(ServerWebExchange, WebFilterChain)` |
| `request.getServletPath()` | `exchange.getRequest().getPath().value()` |
| `request.getHeader("Authorization")` | `exchange.getRequest().getHeaders().getFirst("Authorization")` |
| `SecurityContextHolder.getContext().setAuthentication(...)` | Return `chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(...))` |
| `filterChain.doFilter(request, response)` | `return chain.filter(exchange)` |

### JwtAuthFilter.java (DELETE)
**Path**: `auth-service/src/main/java/com/adventuretube/auth/filter/JwtAuthFilter.java`

### CustomUserDetailService.java (MODIFY)
**Path**: `auth-service/src/main/java/com/adventuretube/auth/service/CustomUserDetailService.java`

| Before | After |
|---|---|
| `implements UserDetailsService` | `implements ReactiveUserDetailsService` |
| `UserDetails loadUserByUsername(String email)` | `Mono<UserDetails> findByUsername(String email)` |
| `serviceClient.post(...)` (blocking) | `serviceClient.postReactive(...)` (returns Mono) |
| Throws exceptions directly | Returns `Mono.error(...)` for error cases |

### CustomAuthenticationProvider.java (DELETE)
**Path**: `auth-service/src/main/java/com/adventuretube/auth/provider/CustomAuthenticationProvider.java`

- `DaoAuthenticationProvider` is servlet-based, doesn't work with WebFlux
- Authentication logic moves into `ReactiveAuthenticationManager` bean in `AuthServiceConfig`:
  - Calls `reactiveUserDetailsService.findByUsername(email)`
  - Compares password with `passwordEncoder.matches()`
  - Returns `UsernamePasswordAuthenticationToken` on success

### SecurityConstants.java (KEEP as-is)
**Path**: `auth-service/src/main/java/com/adventuretube/auth/config/security/SecurityConstants.java`

- `String[]` works with WebFlux `pathMatchers(String...)` — no change needed

### Unit Test: `SecurityConfigTest.java`
**Path**: `auth-service/src/test/java/com/adventuretube/auth/unit/security/SecurityConfigTest.java`

- Use `@WebFluxTest` with `WebTestClient`
- Mock `ReactiveUserDetailsService`, `JwtUtil`
- Test open endpoints return 200 without auth token
- Test protected endpoints return 401 without auth token
- Test protected endpoints return 200 with valid JWT

### Unit Test: `JwtWebFilterTest.java`
**Path**: `auth-service/src/test/java/com/adventuretube/auth/unit/security/JwtWebFilterTest.java`

- Mock `JwtUtil`, `ReactiveUserDetailsService`
- Test filter passes through for open endpoints
- Test filter extracts and validates JWT from Authorization header
- Test filter sets authentication in reactive security context
- Test filter handles missing/invalid tokens gracefully

### Verification
- `mvn compile -pl auth-service -am` — no errors
- `mvn test -pl auth-service` — SecurityConfigTest + JwtWebFilterTest pass

---

## Step 2: Controller-Service Layer (Annotation → Functional Reactive)

### AuthService.java (MODIFY)
**Path**: `auth-service/src/main/java/com/adventuretube/auth/service/AuthService.java`

| Method | Before | After |
|---|---|---|
| `createUser(MemberRegisterRequest)` | Returns `MemberRegisterResponse` | Returns `Mono<MemberRegisterResponse>` |
| `issueToken(MemberLoginRequest)` | Returns `MemberRegisterResponse` | Returns `Mono<MemberRegisterResponse>` |
| `revokeToken(HttpServletRequest)` | Returns `ServiceResponse` | `revokeToken(String token)` → `Mono<ServiceResponse>` |
| `refreshToken(HttpServletRequest)` | Returns `MemberRegisterResponse` | `refreshToken(String token)` → `Mono<MemberRegisterResponse>` |

Key changes:
- Replace `serviceClient.post()` → `serviceClient.postReactive()`, chain with `.flatMap()`
- Remove `HttpServletRequest` parameter from `revokeToken` and `refreshToken` — token extraction moves to router handler
- `verifyGoogleIdToken()` is CPU-bound → wrap with `Mono.fromCallable().subscribeOn(Schedulers.boundedElastic())`
- `authenticationManager.authenticate()` → use `reactiveAuthenticationManager.authenticate()` returning `Mono<Authentication>`
- Remove `@Transactional` — auth-service has no database

### AuthRouterConfig.java (NEW — replaces AuthController.java)
**Path**: `auth-service/src/main/java/com/adventuretube/auth/controller/AuthRouterConfig.java`

Single `@Configuration` class with routing + handler methods (same pattern as member-service's `MemberRouterConfig`):

```
@Bean RouterFunction<ServerResponse> authRoutes()
    POST /auth/users        → this::registerUser
    POST /auth/token        → this::issueToken
    POST /auth/token/refresh → this::refreshToken
    POST /auth/token/revoke  → this::revokeToken
```

Handler methods: `ServerRequest → Mono<ServerResponse>`
- `registerUser`: `request.bodyToMono(MemberRegisterRequest.class)` → call `authService.createUser()` → `ServerResponse.created(uri).bodyValue(...)`
- `issueToken`: `request.bodyToMono(MemberLoginRequest.class)` → call `authService.issueToken()` → `ServerResponse.ok().bodyValue(...)`
- `refreshToken`: extract `Authorization` header from `request.headers()` → call `authService.refreshToken(token)` → `ServerResponse.ok().bodyValue(...)`
- `revokeToken`: extract `Authorization` header from `request.headers()` → call `authService.revokeToken(token)` → `ServerResponse.ok().bodyValue(...)`

### AuthController.java (DELETE)
**Path**: `auth-service/src/main/java/com/adventuretube/auth/controller/AuthController.java`

### Unit Test: `AuthRouterConfigTest.java`
**Path**: `auth-service/src/test/java/com/adventuretube/auth/unit/controller/AuthRouterConfigTest.java`

- Use `WebTestClient.bindToRouterFunction(...)` with mocked `AuthService`
- Test `POST /auth/users` — mock `authService.createUser()` → verify 201 + response body
- Test `POST /auth/token` — mock `authService.issueToken()` → verify 200 + response body
- Test `POST /auth/token/refresh` — set Authorization header → verify 200
- Test `POST /auth/token/revoke` — set Authorization header → verify 200

### Unit Test: `AuthServiceTest.java`
**Path**: `auth-service/src/test/java/com/adventuretube/auth/unit/service/AuthServiceTest.java`

- Mock `ServiceClient` (postReactive returns predefined `Mono<ServiceResponse>`)
- Mock `JwtUtil`, `PasswordEncoder`, `ReactiveAuthenticationManager`
- Test `createUser()` — happy path returns `Mono<MemberRegisterResponse>`
- Test `createUser()` — duplicate email → `Mono.error(DuplicateException)`
- Test `issueToken()` — happy path
- Test `revokeToken()` — happy path + token not found
- Test `refreshToken()` — happy path + token not found
- Use `StepVerifier` to assert reactive streams

### Verification
- `mvn compile -pl auth-service -am` — no errors
- `mvn test -pl auth-service` — AuthRouterConfigTest + AuthServiceTest pass
- Same URL paths: `/auth/users`, `/auth/token`, `/auth/token/refresh`, `/auth/token/revoke`
- Same response JSON format — no gateway or client changes needed

---

## Step 3: Exception Handling

### GlobalWebExceptionHandler.java (NEW)
**Path**: `auth-service/src/main/java/com/adventuretube/auth/exceptions/global/GlobalWebExceptionHandler.java`

- `@ControllerAdvice` does NOT work with functional endpoints → use `WebExceptionHandler`
- `@Component @Order(-2)` to run before Spring's default error handler
- Inject `ObjectMapper` for JSON serialization
- Handle all 15 exception types from current `GlobalExceptionHandler`:
  - `MethodArgumentNotValidException` → 400 (note: may not fire with functional endpoints — validation moves to handler)
  - `UsernameNotFoundException` → 404
  - `BadCredentialsException` → 401
  - `InternalAuthenticationServiceException` → 404
  - `GoogleIdTokenInvalidException` → 401
  - `GeneralSecurityException` → 401
  - `TokenSaveFailedException` → 500
  - `TokenDeletionException` → 500
  - `TokenNotFoundException` → 404
  - `TokenExpiredException` → 401
  - `DuplicateException` → 409
  - `MemberServiceException` → 500
  - `IllegalStateException` → 500
  - `InternalServerException` → 500
  - `Exception` (fallback) → 500
- Same `ServiceResponse` JSON format as current handler
- Writes response via `exchange.getResponse().writeWith(...)` (same as member-service's `GlobalWebExceptionHandler`)

### GlobalExceptionHandler.java (DELETE)
**Path**: `auth-service/src/main/java/com/adventuretube/auth/exceptions/global/GlobalExceptionHandler.java`

### Validation Note
- With functional endpoints, `@Valid` annotation doesn't auto-trigger `MethodArgumentNotValidException`
- Validation must be done manually in handler methods using `Validator` bean, or handled in `AuthService`
- Alternatively, use Spring's `Validator` and call `validate()` explicitly in the handler

### Unit Test: `GlobalWebExceptionHandlerTest.java`
**Path**: `auth-service/src/test/java/com/adventuretube/auth/unit/exceptions/GlobalWebExceptionHandlerTest.java`

- Use `WebTestClient` bound to router + exception handler
- Mock `AuthService` to throw each exception type
- Test `DuplicateException` → 409 + correct `ServiceResponse` JSON
- Test `GoogleIdTokenInvalidException` → 401
- Test `TokenNotFoundException` → 404
- Test `BadCredentialsException` → 401
- Test fallback `Exception` → 500
- Verify response body contains `success: false`, `errorCode`, `message`

### Verification
- `mvn compile -pl auth-service -am` — no errors
- `mvn test -pl auth-service` — GlobalWebExceptionHandlerTest passes
- Error responses return same `ServiceResponse` JSON format with correct HTTP status codes

---

## Step 4: Swagger (WebMVC → WebFlux)

### pom.xml (MODIFY)
**Path**: `auth-service/pom.xml`

| Remove | Add |
|--------|-----|
| `springdoc-openapi-starter-webmvc-ui` | `springdoc-openapi-starter-webflux-ui` |

### AuthRouterConfig.java (MODIFY — add Swagger annotations)
**Path**: `auth-service/src/main/java/com/adventuretube/auth/controller/AuthRouterConfig.java`

Add `@RouterOperations` on the `@Bean` method to preserve OpenAPI documentation:

```java
@RouterOperations({
    @RouterOperation(
        path = "/auth/users", method = RequestMethod.POST,
        operation = @Operation(
            summary = "Create new user and issue tokens",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = MemberRegisterRequest.class))),
            responses = {
                @ApiResponse(responseCode = "201", description = "User registered successfully"),
                @ApiResponse(responseCode = "401", description = "Invalid Google ID token"),
                @ApiResponse(responseCode = "409", description = "Email already exists"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
            }
        )
    ),
    // ... similar for /auth/token, /auth/token/refresh, /auth/token/revoke
})
@Bean
public RouterFunction<ServerResponse> authRoutes() { ... }
```

### SwaggerConfig.java (KEEP as-is)
**Path**: `auth-service/src/main/java/com/adventuretube/auth/config/SwaggerConfig.java`

- `OpenAPI` bean works for both webmvc and webflux — no change needed

### Unit Test: `SwaggerConfigTest.java`
**Path**: `auth-service/src/test/java/com/adventuretube/auth/unit/swagger/SwaggerConfigTest.java`

- Use `WebTestClient` to verify `/v3/api-docs` returns valid OpenAPI JSON
- Verify all 4 endpoints are documented in the spec
- Verify `/swagger-ui.html` redirects/loads successfully

### Verification
- `mvn compile -pl auth-service -am` — no errors
- `mvn test -pl auth-service` — SwaggerConfigTest passes
- Swagger UI accessible at `/swagger-ui.html`
- All 4 endpoints documented with request/response schemas

---

## Full File Summary

| Step | File | Action |
|------|------|--------|
| 0 | `common-api/.../ServiceClient.java` | MODIFY (add `postReactive`, `getReactive`) |
| 0 | `auth-service/pom.xml` | MODIFY (remove web, jpa, postgresql) |
| 1 | `auth-service/.../AuthServiceConfig.java` | MODIFY (WebFlux security) |
| 1 | `auth-service/.../JwtWebFilter.java` | NEW (replaces JwtAuthFilter) |
| 1 | `auth-service/.../JwtAuthFilter.java` | DELETE |
| 1 | `auth-service/.../CustomUserDetailService.java` | MODIFY (ReactiveUserDetailsService) |
| 1 | `auth-service/.../CustomAuthenticationProvider.java` | DELETE |
| 2 | `auth-service/.../AuthService.java` | MODIFY (return Mono, use postReactive) |
| 2 | `auth-service/.../AuthRouterConfig.java` | NEW (functional router + handlers) |
| 2 | `auth-service/.../AuthController.java` | DELETE |
| 3 | `auth-service/.../GlobalWebExceptionHandler.java` | NEW (WebExceptionHandler) |
| 3 | `auth-service/.../GlobalExceptionHandler.java` | DELETE |
| 4 | `auth-service/pom.xml` | MODIFY (swap webmvc-ui → webflux-ui) |
| 4 | `auth-service/.../AuthRouterConfig.java` | MODIFY (add @RouterOperations) |
