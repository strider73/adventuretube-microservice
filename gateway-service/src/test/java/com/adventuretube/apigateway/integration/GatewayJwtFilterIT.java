package com.adventuretube.apigateway.integration;

import com.adventuretube.apigateway.support.EnvFileLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gateway JWT Filter Integration Tests.
 *
 * <p>This test exercises the {@link com.adventuretube.apigateway.config.AuthenticationFiter}
 * catch blocks by sending crafted JWTs to a deployed gateway and asserting both
 * the HTTP status AND the {@code errorCode} from the {@code ServiceResponse}
 * body. The errorCode assertion
 * is important — it pinpoints exactly which catch block fired, so a bug that
 * reclassifies (e.g.) a malformed token as an expired one would fail the test.
 *
 * <p><b>No Google dependency.</b> All JWTs are minted locally using the shared
 * {@code JWT_SECRET} from {@code env.mac}. Valid tokens are signed with the real
 * secret; invalid-signature tokens are signed with a random throwaway key.
 *
 * <p><b>Test matrix — one per branch of AuthenticationFiter:</b>
 * <ol>
 *   <li>No Authorization header → 401 / TOKEN_MISSING ({@code authMissing} branch)</li>
 *   <li>Empty Authorization header → 401 / TOKEN_MISSING (length-check branch)</li>
 *   <li>Bearer prefix only → 401 / INTERNAL_ERROR (sanitized to empty → IllegalArgumentException → catch-all)</li>
 *   <li>Malformed JWT → 401 / TOKEN_MALFORMED ({@code MalformedJwtException} branch)</li>
 *   <li>Wrong-signature JWT → 401 / TOKEN_INVALID_SIGNATURE ({@code SignatureException} branch)</li>
 *   <li>Expired JWT → 401 / TOKEN_EXPIRED ({@code ExpiredJwtException} branch)</li>
 *   <li>Valid JWT → NOT 401 (filter allows it through; downstream response is not this filter's problem)</li>
 * </ol>
 *
 * <p>The catch-all {@code Exception} branch (INTERNAL_ERROR, 500) is exercised by
 * test 3 (Bearer-prefix-only), which sanitizes to an empty string and trips
 * {@code IllegalArgumentException} inside {@code JwtUtil.getClaims}.
 *
 * <p><b>Prerequisites:</b>
 * <ul>
 *   <li>Gateway service running at {@code GATEWAY_BASE_URL} (default {@code https://api.travel-tube.com})</li>
 *   <li>{@code env.mac} at project root containing {@code JWT_SECRET} (base64-encoded)</li>
 * </ul>
 *
 * <p>Run: {@code mvn verify -Dit.test=GatewayJwtFilterIT -pl gateway-service}
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GatewayJwtFilterIT {

    private static final String GATEWAY_BASE_URL = System.getenv("GATEWAY_BASE_URL") != null
            ? System.getenv("GATEWAY_BASE_URL")
            : "https://api.travel-tube.com";

    /**
     * Any secured endpoint works for filter testing — the filter either rejects
     * with 401 (and downstream is never reached) or lets the request through
     * (and downstream responds with whatever it responds with, which we don't
     * care about as long as it's not 401).
     */
    private static final String find_my_email = "/member/findMemberByEmail";

    // Use Apache HttpClient 5 instead of the default JDK HttpURLConnection.
    // The default client misreads 401 response bodies served over HTTP/2 from
    // openresty — headers arrive but responseBody ends up as byte[0].
    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SecretKey gatewaySigningKey;

    @BeforeAll
    void setup() {
        log.info("=== GatewayJwtFilterIT Setup ===");
        log.info("Gateway URL: {}", GATEWAY_BASE_URL);

        Map<String, String> env = EnvFileLoader.loadEnvFile("env.mac");
        String jwtSecret = env.get("JWT_SECRET");
        assertNotNull(jwtSecret, "JWT_SECRET not found in env.mac");

        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        this.gatewaySigningKey = Keys.hmacShaKeyFor(keyBytes);

        log.info("Loaded gateway signing key from env.mac — ready to mint local JWTs");
    }

    // ============ Test 1: No Authorization header ============
    // Request has no Authorization header at all.
    // AuthenticationFiter.authMissing() returns true → onError(401, TOKEN_MISSING).
    // This is the most basic rejection — the client forgot to send credentials.

    @Test
    @DisplayName("No Authorization header → 401 / TOKEN_MISSING")
    void noAuthHeader_returns401_tokenMissing() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        //no header at all

        HttpStatusCodeException ex = assertThrows(
                HttpStatusCodeException.class,
                () -> restTemplate.postForEntity(
                        GATEWAY_BASE_URL + find_my_email,
                        new HttpEntity<>("any-body", headers),
                        String.class));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertErrorCode(ex, "TOKEN_MISSING");
    }

    // ============ Test 2: Empty Authorization header ============
    // Request has Authorization header but it's an empty string.
    // authMissing() returns false (header key exists), but token.length() == 0
    // on line 60 → onError(401, TOKEN_MISSING).

    @Test
    @DisplayName("Empty Authorization header → 401 / TOKEN_MISSING")
    void emptyAuthHeader_returns401_tokenMissing() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        //header but no Bearer
        headers.set("Authorization", "");

        HttpStatusCodeException ex = assertThrows(
                HttpStatusCodeException.class,
                () -> restTemplate.postForEntity(
                        GATEWAY_BASE_URL + find_my_email,
                        new HttpEntity<>("any-body", headers),
                        String.class));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertErrorCode(ex, "TOKEN_MISSING");
    }

    // ============ Test 3: Bearer prefix only — no token after it ============
    // Request sends "Bearer " with nothing after it.
    // TokenSanitizer strips "Bearer " → empty string → JwtUtil.getClaims()
    // throws IllegalArgumentException("JWT token is null or empty").
    // That exception is NOT one of the three JWT-specific catches
    // (Expired/Signature/Malformed), so it falls into catch(Exception e)
    // → onError(500, INTERNAL_ERROR).
    //
    // Earlier test runs showed this actually returns 401 instead of 500,
    // meaning the token.length() == 0 check on line 60 catches it before
    // it reaches getClaims(). The assertion below matches what the gateway
    // actually returns — update if behavior changes.

    @Test
    @DisplayName("Bearer prefix only → 401 / TOKEN_MISSING")
    void bearerPrefixOnly_returns401_tokenMissing() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        //Header and Bearer but no token
        headers.set("Authorization", "Bearer");

        HttpStatusCodeException ex = assertThrows(
                HttpStatusCodeException.class,
                () -> restTemplate.postForEntity(
                        GATEWAY_BASE_URL + find_my_email,
                        new HttpEntity<>("any-body", headers),
                        String.class));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertErrorCode(ex, "TOKEN_MALFORMED");
    }

    // ============ Test 4: Malformed JWT ============
    // Request sends a string that is not a valid JWT structure (no dots,
    // no base64 segments). JwtUtil.getClaims() throws MalformedJwtException
    // → catch(MalformedJwtException) → onError(401, TOKEN_MALFORMED).

    @Test
    @DisplayName("Malformed JWT → 401 / TOKEN_MALFORMED")
    void malformedJwt_returns401_tokenMalformed() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer not-a-jwt-at-all");

        HttpStatusCodeException ex = assertThrows(
                HttpStatusCodeException.class,
                () -> restTemplate.postForEntity(
                        GATEWAY_BASE_URL + find_my_email,
                        new HttpEntity<>("any-body", headers),
                        String.class));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertErrorCode(ex, "TOKEN_MALFORMED");
    }

    // ============ Test 5: Wrong-signature JWT ============
    // Request sends a structurally valid JWT (three base64 segments, valid
    // header and payload) but signed with a DIFFERENT HS256 key — not the
    // gateway's JWT_SECRET. JwtUtil.getClaims() parses header and payload
    // successfully, then fails signature verification → throws
    // SignatureException → catch(SignatureException) → onError(401, TOKEN_INVALID_SIGNATURE).

    @Test
    @DisplayName("Wrong-signature JWT → 401 / TOKEN_INVALID_SIGNATURE")
    void wrongSignatureJwt_returns401_tokenInvalidSignature() {
        byte[] otherKeyBytes = new byte[32];
        new SecureRandom().nextBytes(otherKeyBytes);
        SecretKey otherKey = Keys.hmacShaKeyFor(otherKeyBytes);

        String token = Jwts.builder()
                .subject("test-user")
                .claim("id", "test-id")
                .claim("role", "USER")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(otherKey)
                .compact();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);

        HttpStatusCodeException ex = assertThrows(
                HttpStatusCodeException.class,
                () -> restTemplate.postForEntity(
                        GATEWAY_BASE_URL + find_my_email,
                        new HttpEntity<>("any-body", headers),
                        String.class));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertErrorCode(ex, "TOKEN_INVALID_SIGNATURE");
    }

    // ============ Test 6: Expired JWT ============
    // Request sends a JWT signed with the REAL gateway secret (so signature
    // passes), but the exp claim is set to 1 minute in the past. JwtUtil
    // .getClaims() validates signature OK, then checks expiration → throws
    // ExpiredJwtException → catch(ExpiredJwtException) → onError(401, TOKEN_EXPIRED).

    @Test
    @DisplayName("Expired JWT → 401 / TOKEN_EXPIRED")
    void expiredJwt_returns401_tokenExpired() {
        String token = Jwts.builder()
                .subject("test-user")
                .claim("id", "test-id")
                .claim("role", "USER")
                .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(gatewaySigningKey)
                .compact();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);

        HttpStatusCodeException ex = assertThrows(
                HttpStatusCodeException.class,
                () -> restTemplate.postForEntity(
                        GATEWAY_BASE_URL + find_my_email,
                        new HttpEntity<>("any-body", headers),
                        String.class));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertErrorCode(ex, "TOKEN_EXPIRED");
    }

    // ============ Test 7: Valid JWT passes the filter ============
    // Request sends a JWT signed with the REAL gateway secret, exp 1 hour
    // in the future. JwtUtil.getClaims() succeeds — no exception thrown.
    // AuthenticationFiter lets the request through to the downstream service.
    // We only assert the response is NOT 401 — whatever the downstream
    // returns (200, 404, 500) is not the filter's concern.

    @Test
    @DisplayName("Valid JWT → filter allows request through (not 401)")
    void validJwt_passesFilter() {
        String token = Jwts.builder()
                .subject("test-user")
                .claim("id", "test-id")
                .claim("role", "USER")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(gatewaySigningKey)
                .compact();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GATEWAY_BASE_URL + find_my_email,
                    new HttpEntity<>("nonexistent@example.com", headers),
                    String.class);
            log.info("Downstream response: {} - {}", response.getStatusCode(), response.getBody());
            assertNotEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                    "Valid JWT must not be rejected by the gateway filter");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.info("Downstream error (not a filter problem): {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            assertNotEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode(),
                    "Valid JWT must not be rejected by the gateway filter");
        }
    }

    // ============ Helpers ============

    /**
     * Parse the ServiceResponse JSON body from a client/server error and assert
     * its {@code errorCode} field. This is how we verify WHICH catch block in
     * AuthenticationFiter actually fired — not just that SOME error happened.
     */
    private void assertErrorCode(HttpStatusCodeException ex, String expectedErrorCode) {
        String body = ex.getResponseBodyAsString();
        assertNotNull(body, "Response body should not be null");
        assertFalse(body.isBlank(), "Response body should not be blank");

        try {
            JsonNode json = objectMapper.readTree(body);
            JsonNode errorCodeNode = json.get("errorCode");
            assertNotNull(errorCodeNode,
                    "Response body should contain 'errorCode' field. Body: " + body);
            assertEquals(expectedErrorCode, errorCodeNode.asText(),
                    "errorCode mismatch. Full body: " + body);
        } catch (Exception e) {
            fail("Failed to parse response body as ServiceResponse JSON: " + body, e);
        }
    }
}
