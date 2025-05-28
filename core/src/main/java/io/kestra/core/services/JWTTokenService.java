package io.kestra.core.services;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.kestra.core.models.rbac.User;
import io.kestra.core.security.SSOConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for JWT token management and validation
 */
@Singleton
@Slf4j
public class JWTTokenService {

    private final SSOConfig ssoConfig;
    private final SecretKey signingKey;

    // Token blacklist for logout functionality
    private final ConcurrentMap<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    // Refresh token storage
    private final ConcurrentMap<String, RefreshTokenInfo> refreshTokens = new ConcurrentHashMap<>();

    // Token type constants
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String TOKEN_TYPE_ID = "id";

    @Inject
    public JWTTokenService(SSOConfig ssoConfig) {
        this.ssoConfig = ssoConfig;
        this.signingKey = Keys.hmacShaKeyFor("your-256-bit-secret-key-here-must-be-at-least-32-characters".getBytes());
    }

    /**
     * Generate access token for user
     */
    public String generateAccessToken(User user, String tenantId) {
        return generateToken(user, tenantId, TOKEN_TYPE_ACCESS, Duration.ofHours(1));
    }

    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(User user, String tenantId) {
        String refreshToken = generateToken(user, tenantId, TOKEN_TYPE_REFRESH, Duration.ofDays(30));

        // Store refresh token info
        RefreshTokenInfo tokenInfo = new RefreshTokenInfo(
            user.getId(),
            tenantId,
            Instant.now().plus(Duration.ofDays(30))
        );
        refreshTokens.put(refreshToken, tokenInfo);

        return refreshToken;
    }

    /**
     * Generate ID token for user (OIDC)
     */
    public String generateIdToken(User user, String tenantId, Map<String, Object> claims) {
        JwtBuilder builder = Jwts.builder()
            .subject(user.getId())
            .issuer("dataflare")
            .audience().add(ssoConfig.getOidc().getClientId()).and()
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
            .claim("typ", TOKEN_TYPE_ID)
            .claim("tenant_id", tenantId)
            .claim("username", user.getUsername())
            .claim("email", user.getEmail())
            .claim("name", user.getFirstName() + " " + user.getLastName())
            .claim("preferred_username", user.getUsername())
            .claim("given_name", user.getFirstName())
            .claim("family_name", user.getLastName())
            .claim("roles", user.getRoles())
            .claim("permissions", user.getPermissions());

        // Add custom claims
        if (claims != null) {
            claims.forEach(builder::claim);
        }

        return builder.signWith(signingKey).compact();
    }

    /**
     * Generate token with specified type and duration
     */
    private String generateToken(User user, String tenantId, String tokenType, Duration duration) {
        Instant now = Instant.now();
        Instant expiration = now.plus(duration);

        return Jwts.builder()
            .subject(user.getId())
            .issuer("dataflare")
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .claim("typ", tokenType)
            .claim("tenant_id", tenantId)
            .claim("username", user.getUsername())
            .claim("email", user.getEmail())
            .claim("roles", user.getRoles())
            .claim("permissions", user.getPermissions())
            .claim("jti", UUID.randomUUID().toString()) // JWT ID for blacklisting
            .signWith(signingKey)
            .compact();
    }

    /**
     * Validate and parse JWT token
     */
    public TokenValidationResult validateToken(String token) {
        try {
            // Check if token is blacklisted
            if (isTokenBlacklisted(token)) {
                return TokenValidationResult.invalid("Token has been revoked");
            }

            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            // Check expiration
            if (claims.getExpiration().before(new Date())) {
                return TokenValidationResult.invalid("Token has expired");
            }

            return TokenValidationResult.valid(claims);

        } catch (ExpiredJwtException e) {
            log.debug("Token expired: {}", e.getMessage());
            return TokenValidationResult.invalid("Token has expired");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return TokenValidationResult.invalid("Unsupported token format");
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return TokenValidationResult.invalid("Malformed token");
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return TokenValidationResult.invalid("Invalid token signature");
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            return TokenValidationResult.invalid("Invalid token");
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public TokenRefreshResult refreshAccessToken(String refreshToken) {
        TokenValidationResult validationResult = validateToken(refreshToken);

        if (!validationResult.isValid()) {
            return TokenRefreshResult.invalid("Invalid refresh token");
        }

        Claims claims = validationResult.getClaims();
        String tokenType = claims.get("typ", String.class);

        if (!TOKEN_TYPE_REFRESH.equals(tokenType)) {
            return TokenRefreshResult.invalid("Token is not a refresh token");
        }

        // Check if refresh token exists in storage
        RefreshTokenInfo tokenInfo = refreshTokens.get(refreshToken);
        if (tokenInfo == null) {
            return TokenRefreshResult.invalid("Refresh token not found");
        }

        if (tokenInfo.getExpiresAt().isBefore(Instant.now())) {
            refreshTokens.remove(refreshToken);
            return TokenRefreshResult.invalid("Refresh token has expired");
        }

        // Create user object from claims
        User user = createUserFromClaims(claims);
        String tenantId = claims.get("tenant_id", String.class);

        // Generate new access token
        String newAccessToken = generateAccessToken(user, tenantId);

        return TokenRefreshResult.success(newAccessToken, refreshToken);
    }

    /**
     * Revoke token (add to blacklist)
     */
    public void revokeToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String jti = claims.get("jti", String.class);
            if (jti != null) {
                blacklistedTokens.put(jti, claims.getExpiration().toInstant());
                log.debug("Token revoked: {}", jti);
            }
        } catch (Exception e) {
            log.warn("Failed to revoke token: {}", e.getMessage());
        }
    }

    /**
     * Revoke refresh token
     */
    public void revokeRefreshToken(String refreshToken) {
        refreshTokens.remove(refreshToken);
        revokeToken(refreshToken);
    }

    /**
     * Revoke all tokens for user
     */
    public void revokeAllUserTokens(String userId) {
        // Remove all refresh tokens for user
        refreshTokens.entrySet().removeIf(entry ->
            entry.getValue().getUserId().equals(userId));

        log.info("Revoked all tokens for user: {}", userId);
    }

    /**
     * Check if token is blacklisted
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            String jti = claims.get("jti", String.class);
            return jti != null && blacklistedTokens.containsKey(jti);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clean up expired tokens from blacklist and refresh token storage
     */
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();

        // Clean up blacklisted tokens
        blacklistedTokens.entrySet().removeIf(entry ->
            entry.getValue().isBefore(now));

        // Clean up expired refresh tokens
        refreshTokens.entrySet().removeIf(entry ->
            entry.getValue().getExpiresAt().isBefore(now));

        log.debug("Cleaned up expired tokens. Blacklisted: {}, Refresh: {}",
            blacklistedTokens.size(), refreshTokens.size());
    }

    /**
     * Get token statistics
     */
    public TokenStatistics getTokenStatistics() {
        return new TokenStatistics(
            blacklistedTokens.size(),
            refreshTokens.size(),
            refreshTokens.values().stream()
                .mapToLong(info -> info.getExpiresAt().getEpochSecond())
                .max()
                .orElse(0)
        );
    }

    /**
     * Create user object from JWT claims
     */
    private User createUserFromClaims(Claims claims) {
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        Set<String> roleSet = roles != null ? new HashSet<>(roles) : Set.of();

        @SuppressWarnings("unchecked")
        List<String> permissions = claims.get("permissions", List.class);
        Set<String> permissionSet = permissions != null ? new HashSet<>(permissions) : Set.of();

        return User.builder()
            .id(claims.getSubject())
            .username(claims.get("username", String.class))
            .email(claims.get("email", String.class))
            .roles(roleSet)
            .permissions(permissionSet)
            .build();
    }

    /**
     * Token validation result
     */
    public static class TokenValidationResult {
        private final boolean valid;
        private final String error;
        private final Claims claims;

        private TokenValidationResult(boolean valid, String error, Claims claims) {
            this.valid = valid;
            this.error = error;
            this.claims = claims;
        }

        public static TokenValidationResult valid(Claims claims) {
            return new TokenValidationResult(true, null, claims);
        }

        public static TokenValidationResult invalid(String error) {
            return new TokenValidationResult(false, error, null);
        }

        public boolean isValid() { return valid; }
        public String getError() { return error; }
        public Claims getClaims() { return claims; }
    }

    /**
     * Token refresh result
     */
    public static class TokenRefreshResult {
        private final boolean success;
        private final String error;
        private final String accessToken;
        private final String refreshToken;

        private TokenRefreshResult(boolean success, String error, String accessToken, String refreshToken) {
            this.success = success;
            this.error = error;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public static TokenRefreshResult success(String accessToken, String refreshToken) {
            return new TokenRefreshResult(true, null, accessToken, refreshToken);
        }

        public static TokenRefreshResult invalid(String error) {
            return new TokenRefreshResult(false, error, null, null);
        }

        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }

    /**
     * Refresh token information
     */
    private static class RefreshTokenInfo {
        private final String userId;
        private final String tenantId;
        private final Instant expiresAt;

        public RefreshTokenInfo(String userId, String tenantId, Instant expiresAt) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.expiresAt = expiresAt;
        }

        public String getUserId() { return userId; }
        public String getTenantId() { return tenantId; }
        public Instant getExpiresAt() { return expiresAt; }
    }

    /**
     * Token statistics
     */
    public static class TokenStatistics {
        public final int blacklistedTokensCount;
        public final int refreshTokensCount;
        public final long latestRefreshTokenExpiry;

        public TokenStatistics(int blacklistedTokensCount, int refreshTokensCount, long latestRefreshTokenExpiry) {
            this.blacklistedTokensCount = blacklistedTokensCount;
            this.refreshTokensCount = refreshTokensCount;
            this.latestRefreshTokenExpiry = latestRefreshTokenExpiry;
        }
    }
}
