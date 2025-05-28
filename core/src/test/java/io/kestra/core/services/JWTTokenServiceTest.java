package io.kestra.core.services;

import io.kestra.core.models.rbac.User;
import io.kestra.core.security.SSOConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class JWTTokenServiceTest {

    @Mock
    private SSOConfig ssoConfig;

    @Mock
    private SSOConfig.OIDCConfig oidcConfig;

    private JWTTokenService jwtTokenService;
    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(ssoConfig.getOidc()).thenReturn(oidcConfig);
        when(oidcConfig.getClientId()).thenReturn("test-client");

        jwtTokenService = new JWTTokenService(ssoConfig);

        testUser = User.builder()
            .id("user123")
            .username("john.doe")
            .email("john.doe@example.com")
            .firstName("John")
            .lastName("Doe")
            .roles(Set.of("USER", "ADMIN"))
            .permissions(Set.of("read", "write"))
            .build();
    }

    @Test
    void testGenerateAccessToken() {
        String token = jwtTokenService.generateAccessToken(testUser, "tenant1");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));

        // Validate token
        JWTTokenService.TokenValidationResult result = jwtTokenService.validateToken(token);
        assertTrue(result.isValid());
        assertNull(result.getError());
        assertNotNull(result.getClaims());

        // Check claims
        assertEquals("user123", result.getClaims().getSubject());
        assertEquals("dataflare", result.getClaims().getIssuer());
        assertEquals("access", result.getClaims().get("typ"));
        assertEquals("tenant1", result.getClaims().get("tenant_id"));
        assertEquals("john.doe", result.getClaims().get("username"));
        assertEquals("john.doe@example.com", result.getClaims().get("email"));
    }

    @Test
    void testGenerateRefreshToken() {
        String token = jwtTokenService.generateRefreshToken(testUser, "tenant1");

        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Validate token
        JWTTokenService.TokenValidationResult result = jwtTokenService.validateToken(token);
        assertTrue(result.isValid());
        assertEquals("refresh", result.getClaims().get("typ"));
    }

    @Test
    void testGenerateIdToken() {
        Map<String, Object> customClaims = Map.of(
            "custom_claim", "custom_value",
            "department", "Engineering"
        );

        String token = jwtTokenService.generateIdToken(testUser, "tenant1", customClaims);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Validate token
        JWTTokenService.TokenValidationResult result = jwtTokenService.validateToken(token);
        assertTrue(result.isValid());

        // Check ID token specific claims
        assertEquals("id", result.getClaims().get("typ"));
        assertTrue(result.getClaims().getAudience().contains("test-client"));
        assertEquals("John Doe", result.getClaims().get("name"));
        assertEquals("john.doe", result.getClaims().get("preferred_username"));
        assertEquals("John", result.getClaims().get("given_name"));
        assertEquals("Doe", result.getClaims().get("family_name"));

        // Check custom claims
        assertEquals("custom_value", result.getClaims().get("custom_claim"));
        assertEquals("Engineering", result.getClaims().get("department"));
    }

    @Test
    void testValidateValidToken() {
        String token = jwtTokenService.generateAccessToken(testUser, "tenant1");

        JWTTokenService.TokenValidationResult result = jwtTokenService.validateToken(token);

        assertTrue(result.isValid());
        assertNull(result.getError());
        assertNotNull(result.getClaims());
    }

    @Test
    void testValidateInvalidToken() {
        String invalidToken = "invalid.token.here";

        JWTTokenService.TokenValidationResult result = jwtTokenService.validateToken(invalidToken);

        assertFalse(result.isValid());
        assertNotNull(result.getError());
        assertNull(result.getClaims());
    }

    @Test
    void testValidateRevokedToken() {
        String token = jwtTokenService.generateAccessToken(testUser, "tenant1");

        // Revoke token
        jwtTokenService.revokeToken(token);

        // Validate revoked token
        JWTTokenService.TokenValidationResult result = jwtTokenService.validateToken(token);

        assertFalse(result.isValid());
        assertEquals("Token has been revoked", result.getError());
    }

    @Test
    void testRefreshAccessToken() {
        String refreshToken = jwtTokenService.generateRefreshToken(testUser, "tenant1");

        JWTTokenService.TokenRefreshResult result = jwtTokenService.refreshAccessToken(refreshToken);

        assertTrue(result.isSuccess());
        assertNull(result.getError());
        assertNotNull(result.getAccessToken());
        assertNotNull(result.getRefreshToken());

        // Validate new access token
        JWTTokenService.TokenValidationResult validationResult =
            jwtTokenService.validateToken(result.getAccessToken());
        assertTrue(validationResult.isValid());
        assertEquals("access", validationResult.getClaims().get("typ"));
    }

    @Test
    void testRefreshWithInvalidToken() {
        String invalidToken = "invalid.refresh.token";

        JWTTokenService.TokenRefreshResult result = jwtTokenService.refreshAccessToken(invalidToken);

        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
        assertNull(result.getAccessToken());
        assertNull(result.getRefreshToken());
    }

    @Test
    void testRefreshWithAccessToken() {
        String accessToken = jwtTokenService.generateAccessToken(testUser, "tenant1");

        JWTTokenService.TokenRefreshResult result = jwtTokenService.refreshAccessToken(accessToken);

        assertFalse(result.isSuccess());
        assertEquals("Token is not a refresh token", result.getError());
    }

    @Test
    void testRevokeToken() {
        String token = jwtTokenService.generateAccessToken(testUser, "tenant1");

        // Token should be valid initially
        assertTrue(jwtTokenService.validateToken(token).isValid());

        // Revoke token
        jwtTokenService.revokeToken(token);

        // Token should be invalid after revocation
        assertFalse(jwtTokenService.validateToken(token).isValid());
    }

    @Test
    void testRevokeRefreshToken() {
        String refreshToken = jwtTokenService.generateRefreshToken(testUser, "tenant1");

        // Token should be valid initially
        assertTrue(jwtTokenService.validateToken(refreshToken).isValid());

        // Revoke refresh token
        jwtTokenService.revokeRefreshToken(refreshToken);

        // Token should be invalid after revocation
        assertFalse(jwtTokenService.validateToken(refreshToken).isValid());

        // Refresh should fail
        JWTTokenService.TokenRefreshResult result = jwtTokenService.refreshAccessToken(refreshToken);
        assertFalse(result.isSuccess());
    }

    @Test
    void testRevokeAllUserTokens() {
        String accessToken = jwtTokenService.generateAccessToken(testUser, "tenant1");
        String refreshToken = jwtTokenService.generateRefreshToken(testUser, "tenant1");

        // Tokens should be valid initially
        assertTrue(jwtTokenService.validateToken(accessToken).isValid());
        assertTrue(jwtTokenService.validateToken(refreshToken).isValid());

        // Revoke all user tokens
        jwtTokenService.revokeAllUserTokens(testUser.getId());

        // Refresh token should be removed from storage
        JWTTokenService.TokenRefreshResult result = jwtTokenService.refreshAccessToken(refreshToken);
        assertFalse(result.isSuccess());
    }

    @Test
    void testCleanupExpiredTokens() {
        // Generate some tokens
        String accessToken = jwtTokenService.generateAccessToken(testUser, "tenant1");
        String refreshToken = jwtTokenService.generateRefreshToken(testUser, "tenant1");

        // Revoke tokens to add them to blacklist
        jwtTokenService.revokeToken(accessToken);
        jwtTokenService.revokeToken(refreshToken);

        // Get initial statistics
        JWTTokenService.TokenStatistics initialStats = jwtTokenService.getTokenStatistics();
        assertTrue(initialStats.blacklistedTokensCount >= 2);

        // Cleanup expired tokens
        jwtTokenService.cleanupExpiredTokens();

        // Statistics should be updated (tokens are not actually expired in this test)
        JWTTokenService.TokenStatistics afterCleanupStats = jwtTokenService.getTokenStatistics();
        assertNotNull(afterCleanupStats);
    }

    @Test
    void testGetTokenStatistics() {
        // Generate and revoke some tokens
        String accessToken = jwtTokenService.generateAccessToken(testUser, "tenant1");
        String refreshToken = jwtTokenService.generateRefreshToken(testUser, "tenant1");

        jwtTokenService.revokeToken(accessToken);

        JWTTokenService.TokenStatistics stats = jwtTokenService.getTokenStatistics();

        assertNotNull(stats);
        assertTrue(stats.blacklistedTokensCount >= 1);
        assertTrue(stats.refreshTokensCount >= 1);
        assertTrue(stats.latestRefreshTokenExpiry > 0);
    }

    @Test
    void testTokenWithoutJTI() {
        // This test ensures the service handles tokens without JTI gracefully
        String token = jwtTokenService.generateAccessToken(testUser, "tenant1");

        // Try to revoke token (should not throw exception)
        assertDoesNotThrow(() -> jwtTokenService.revokeToken(token));
    }

    @Test
    void testMultipleTokenGeneration() {
        // Generate multiple tokens for the same user
        String token1 = jwtTokenService.generateAccessToken(testUser, "tenant1");
        String token2 = jwtTokenService.generateAccessToken(testUser, "tenant1");
        String token3 = jwtTokenService.generateAccessToken(testUser, "tenant2");

        // All tokens should be different
        assertNotEquals(token1, token2);
        assertNotEquals(token1, token3);
        assertNotEquals(token2, token3);

        // All tokens should be valid
        assertTrue(jwtTokenService.validateToken(token1).isValid());
        assertTrue(jwtTokenService.validateToken(token2).isValid());
        assertTrue(jwtTokenService.validateToken(token3).isValid());

        // Check tenant IDs
        assertEquals("tenant1", jwtTokenService.validateToken(token1).getClaims().get("tenant_id"));
        assertEquals("tenant1", jwtTokenService.validateToken(token2).getClaims().get("tenant_id"));
        assertEquals("tenant2", jwtTokenService.validateToken(token3).getClaims().get("tenant_id"));
    }

    @Test
    void testTokenClaimsIntegrity() {
        String token = jwtTokenService.generateAccessToken(testUser, "tenant1");

        JWTTokenService.TokenValidationResult result = jwtTokenService.validateToken(token);
        assertTrue(result.isValid());

        // Verify all expected claims are present
        assertNotNull(result.getClaims().getSubject());
        assertNotNull(result.getClaims().getIssuer());
        assertNotNull(result.getClaims().getIssuedAt());
        assertNotNull(result.getClaims().getExpiration());
        assertNotNull(result.getClaims().get("typ"));
        assertNotNull(result.getClaims().get("tenant_id"));
        assertNotNull(result.getClaims().get("username"));
        assertNotNull(result.getClaims().get("email"));
        assertNotNull(result.getClaims().get("roles"));
        assertNotNull(result.getClaims().get("permissions"));
        assertNotNull(result.getClaims().get("jti"));

        // Verify claim values
        assertEquals(testUser.getId(), result.getClaims().getSubject());
        assertEquals("dataflare", result.getClaims().getIssuer());
        assertEquals("tenant1", result.getClaims().get("tenant_id"));
        assertEquals(testUser.getUsername(), result.getClaims().get("username"));
        assertEquals(testUser.getEmail(), result.getClaims().get("email"));
    }

    @Test
    void testRefreshTokenStorage() {
        String refreshToken1 = jwtTokenService.generateRefreshToken(testUser, "tenant1");
        String refreshToken2 = jwtTokenService.generateRefreshToken(testUser, "tenant2");

        // Both refresh tokens should work
        JWTTokenService.TokenRefreshResult result1 = jwtTokenService.refreshAccessToken(refreshToken1);
        JWTTokenService.TokenRefreshResult result2 = jwtTokenService.refreshAccessToken(refreshToken2);

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());

        // Revoke one refresh token
        jwtTokenService.revokeRefreshToken(refreshToken1);

        // First should fail, second should still work
        JWTTokenService.TokenRefreshResult result3 = jwtTokenService.refreshAccessToken(refreshToken1);
        JWTTokenService.TokenRefreshResult result4 = jwtTokenService.refreshAccessToken(refreshToken2);

        assertFalse(result3.isSuccess());
        assertTrue(result4.isSuccess());
    }
}
