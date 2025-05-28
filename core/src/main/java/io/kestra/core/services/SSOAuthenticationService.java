package io.kestra.core.services;

import io.kestra.core.models.rbac.User;
import io.kestra.core.security.SSOConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for SSO authentication and user management
 */
@Singleton
@Slf4j
public class SSOAuthenticationService {

    private final SSOConfig ssoConfig;
    private final JWTTokenService jwtTokenService;
    private final UserService userService;

    // State storage for OAuth2/OIDC flows
    private final ConcurrentMap<String, AuthenticationState> authenticationStates = new ConcurrentHashMap<>();

    // Session storage
    private final ConcurrentMap<String, UserSession> userSessions = new ConcurrentHashMap<>();

    @Inject
    public SSOAuthenticationService(
        SSOConfig ssoConfig,
        JWTTokenService jwtTokenService,
        UserService userService
    ) {
        this.ssoConfig = ssoConfig;
        this.jwtTokenService = jwtTokenService;
        this.userService = userService;
    }

    /**
     * Initiate SSO authentication
     */
    public AuthenticationInitiation initiateAuthentication(String tenantId, String redirectUri) {
        if (!ssoConfig.isEnabled()) {
            throw new IllegalStateException("SSO authentication is not enabled");
        }

        String state = generateState();
        String nonce = generateNonce();

        // Store authentication state
        AuthenticationState authState = new AuthenticationState(
            tenantId,
            redirectUri,
            state,
            nonce,
            Instant.now().plusSeconds(300) // 5 minutes expiry
        );
        authenticationStates.put(state, authState);

        String authorizationUrl = buildAuthorizationUrl(state, nonce, redirectUri);

        return new AuthenticationInitiation(authorizationUrl, state);
    }

    /**
     * Handle authentication callback
     */
    public AuthenticationResult handleCallback(String code, String state, String error) {
        if (error != null) {
            log.warn("Authentication error: {}", error);
            return AuthenticationResult.error("Authentication failed: " + error);
        }

        if (code == null || state == null) {
            return AuthenticationResult.error("Missing required parameters");
        }

        // Validate state
        AuthenticationState authState = authenticationStates.remove(state);
        if (authState == null) {
            return AuthenticationResult.error("Invalid or expired state");
        }

        if (authState.getExpiresAt().isBefore(Instant.now())) {
            return AuthenticationResult.error("Authentication state expired");
        }

        try {
            // Exchange code for tokens
            TokenResponse tokenResponse = exchangeCodeForTokens(code, authState);

            // Validate and extract user information
            UserInfo userInfo = extractUserInfo(tokenResponse);

            // Create or update user
            User user = createOrUpdateUser(userInfo, authState.getTenantId());

            // Generate application tokens
            String accessToken = jwtTokenService.generateAccessToken(user, authState.getTenantId());
            String refreshToken = jwtTokenService.generateRefreshToken(user, authState.getTenantId());

            // Create user session
            String sessionId = createUserSession(user, authState.getTenantId());

            return AuthenticationResult.success(
                user,
                accessToken,
                refreshToken,
                sessionId,
                authState.getRedirectUri()
            );

        } catch (Exception e) {
            log.error("Authentication callback failed", e);
            return AuthenticationResult.error("Authentication processing failed");
        }
    }

    /**
     * Logout user
     */
    public LogoutResult logout(String sessionId, String accessToken) {
        try {
            // Remove user session
            UserSession session = userSessions.remove(sessionId);

            // Revoke tokens
            if (accessToken != null) {
                jwtTokenService.revokeToken(accessToken);
            }

            if (session != null) {
                jwtTokenService.revokeAllUserTokens(session.getUserId());
            }

            // Build logout URL if supported
            String logoutUrl = buildLogoutUrl();

            return new LogoutResult(true, logoutUrl);

        } catch (Exception e) {
            log.error("Logout failed", e);
            return new LogoutResult(false, null);
        }
    }

    /**
     * Get user session
     */
    public Optional<UserSession> getUserSession(String sessionId) {
        UserSession session = userSessions.get(sessionId);
        if (session != null && session.getExpiresAt().isAfter(Instant.now())) {
            return Optional.of(session);
        }

        // Remove expired session
        if (session != null) {
            userSessions.remove(sessionId);
        }

        return Optional.empty();
    }

    /**
     * Validate user session
     */
    public boolean isSessionValid(String sessionId) {
        return getUserSession(sessionId).isPresent();
    }

    /**
     * Clean up expired states and sessions
     */
    public void cleanupExpired() {
        Instant now = Instant.now();

        // Clean up expired authentication states
        authenticationStates.entrySet().removeIf(entry ->
            entry.getValue().getExpiresAt().isBefore(now));

        // Clean up expired sessions
        userSessions.entrySet().removeIf(entry ->
            entry.getValue().getExpiresAt().isBefore(now));

        log.debug("Cleaned up expired authentication data. States: {}, Sessions: {}",
            authenticationStates.size(), userSessions.size());
    }

    /**
     * Get authentication statistics
     */
    public AuthenticationStatistics getStatistics() {
        return new AuthenticationStatistics(
            authenticationStates.size(),
            userSessions.size(),
            ssoConfig.isEnabled(),
            ssoConfig.getProvider().name()
        );
    }

    /**
     * Build authorization URL for SSO provider
     */
    private String buildAuthorizationUrl(String state, String nonce, String redirectUri) {
        return switch (ssoConfig.getProvider()) {
            case OIDC -> buildOIDCAuthorizationUrl(state, nonce, redirectUri);
            case OAUTH2 -> buildOAuth2AuthorizationUrl(state, redirectUri);
            case SAML -> buildSAMLAuthorizationUrl(state);
            default -> throw new UnsupportedOperationException("Provider not supported: " + ssoConfig.getProvider());
        };
    }

    /**
     * Build OIDC authorization URL
     */
    private String buildOIDCAuthorizationUrl(String state, String nonce, String redirectUri) {
        SSOConfig.OIDCConfig oidc = ssoConfig.getOidc();

        StringBuilder url = new StringBuilder(oidc.getIssuer());
        if (!oidc.getIssuer().endsWith("/")) {
            url.append("/");
        }
        url.append("auth");

        Map<String, String> params = new HashMap<>();
        params.put("response_type", "code");
        params.put("client_id", oidc.getClientId());
        params.put("redirect_uri", redirectUri);
        params.put("scope", String.join(" ", oidc.getScopes()));
        params.put("state", state);
        params.put("nonce", nonce);

        // Add additional authorization parameters
        params.putAll(oidc.getAuthorizationParams());

        return appendQueryParams(url.toString(), params);
    }

    /**
     * Build OAuth2 authorization URL
     */
    private String buildOAuth2AuthorizationUrl(String state, String redirectUri) {
        SSOConfig.OAuth2Config oauth2 = ssoConfig.getOauth2();

        Map<String, String> params = new HashMap<>();
        params.put("response_type", oauth2.getResponseType());
        params.put("client_id", oauth2.getClientId());
        params.put("redirect_uri", redirectUri);
        params.put("scope", String.join(" ", oauth2.getScopes()));
        params.put("state", state);

        return appendQueryParams(oauth2.getAuthorizationUri(), params);
    }

    /**
     * Build SAML authorization URL
     */
    private String buildSAMLAuthorizationUrl(String state) {
        // SAML implementation would go here
        throw new UnsupportedOperationException("SAML not yet implemented");
    }

    /**
     * Exchange authorization code for tokens
     */
    private TokenResponse exchangeCodeForTokens(String code, AuthenticationState authState) {
        // Implementation would depend on the provider
        // This is a simplified version
        return new TokenResponse("access_token", "id_token", "refresh_token", 3600);
    }

    /**
     * Extract user information from token response
     */
    private UserInfo extractUserInfo(TokenResponse tokenResponse) {
        // Implementation would parse the ID token or call userinfo endpoint
        // This is a simplified version
        return new UserInfo(
            "user123",
            "john.doe",
            "john.doe@example.com",
            "John",
            "Doe",
            List.of("user"),
            List.of("read")
        );
    }

    /**
     * Create or update user based on SSO information
     */
    private User createOrUpdateUser(UserInfo userInfo, String tenantId) {
        Optional<User> existingUser = userService.findByUsername(userInfo.getUsername());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (ssoConfig.getUserMapping().isAutoUpdateUsers()) {
                User updatedUser = updateUserFromSSOInfo(user, userInfo);
                return userService.update(updatedUser);
            }
            return user;
        } else if (ssoConfig.getUserMapping().isAutoCreateUsers()) {
            User newUser = createUserFromSSOInfo(userInfo, tenantId);
            return userService.create(newUser);
        } else {
            throw new IllegalStateException("User not found and auto-creation is disabled");
        }
    }

    /**
     * Create user from SSO information
     */
    private User createUserFromSSOInfo(UserInfo userInfo, String tenantId) {
        // Map roles
        Set<String> roles = new HashSet<>(ssoConfig.getUserMapping().getDefaultRoles());
        userInfo.getRoles().forEach(role -> {
            String mappedRole = ssoConfig.getMappedRole(role);
            roles.add(mappedRole);
        });

        return User.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(tenantId)
            .username(userInfo.getUsername())
            .email(userInfo.getEmail())
            .firstName(userInfo.getFirstName())
            .lastName(userInfo.getLastName())
            .enabled(true)
            .roles(roles)
            .permissions(new HashSet<>(userInfo.getPermissions()))
            .build();
    }

    /**
     * Update user from SSO information
     */
    private User updateUserFromSSOInfo(User user, UserInfo userInfo) {
        User.UserBuilder builder = user.toBuilder()
            .email(userInfo.getEmail())
            .firstName(userInfo.getFirstName())
            .lastName(userInfo.getLastName());

        // Update roles if configured
        if (ssoConfig.getUserMapping().isAutoUpdateUsers()) {
            Set<String> roles = new HashSet<>(ssoConfig.getUserMapping().getDefaultRoles());
            userInfo.getRoles().forEach(role -> {
                String mappedRole = ssoConfig.getMappedRole(role);
                roles.add(mappedRole);
            });
            builder.roles(roles);
            builder.permissions(new HashSet<>(userInfo.getPermissions()));
        }

        return builder.build();
    }

    /**
     * Create user session
     */
    private String createUserSession(User user, String tenantId) {
        String sessionId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(ssoConfig.getSession().getTimeout());

        UserSession session = new UserSession(
            sessionId,
            user.getId(),
            tenantId,
            Instant.now(),
            expiresAt
        );

        userSessions.put(sessionId, session);
        return sessionId;
    }

    /**
     * Build logout URL
     */
    private String buildLogoutUrl() {
        return switch (ssoConfig.getProvider()) {
            case OIDC -> ssoConfig.getOidc().getIssuer() + "/logout";
            case OAUTH2 -> null; // OAuth2 doesn't typically have logout
            case SAML -> ssoConfig.getSaml().getSloUrl();
            default -> null;
        };
    }

    /**
     * Generate random state
     */
    private String generateState() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate random nonce
     */
    private String generateNonce() {
        return UUID.randomUUID().toString();
    }

    /**
     * Append query parameters to URL
     */
    private String appendQueryParams(String baseUrl, Map<String, String> params) {
        StringBuilder url = new StringBuilder(baseUrl);
        boolean first = !baseUrl.contains("?");

        for (Map.Entry<String, String> entry : params.entrySet()) {
            url.append(first ? "?" : "&");
            url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            url.append("=");
            url.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }

        return url.toString();
    }

    // Inner classes for data transfer

    public static class AuthenticationInitiation {
        public final String authorizationUrl;
        public final String state;

        public AuthenticationInitiation(String authorizationUrl, String state) {
            this.authorizationUrl = authorizationUrl;
            this.state = state;
        }
    }

    public static class AuthenticationResult {
        private final boolean success;
        private final String error;
        private final User user;
        private final String accessToken;
        private final String refreshToken;
        private final String sessionId;
        private final String redirectUri;

        private AuthenticationResult(boolean success, String error, User user,
                                   String accessToken, String refreshToken,
                                   String sessionId, String redirectUri) {
            this.success = success;
            this.error = error;
            this.user = user;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.sessionId = sessionId;
            this.redirectUri = redirectUri;
        }

        public static AuthenticationResult success(User user, String accessToken,
                                                 String refreshToken, String sessionId,
                                                 String redirectUri) {
            return new AuthenticationResult(true, null, user, accessToken, refreshToken, sessionId, redirectUri);
        }

        public static AuthenticationResult error(String error) {
            return new AuthenticationResult(false, error, null, null, null, null, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public User getUser() { return user; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public String getSessionId() { return sessionId; }
        public String getRedirectUri() { return redirectUri; }
    }

    public static class LogoutResult {
        public final boolean success;
        public final String logoutUrl;

        public LogoutResult(boolean success, String logoutUrl) {
            this.success = success;
            this.logoutUrl = logoutUrl;
        }
    }

    public static class UserSession {
        private final String sessionId;
        private final String userId;
        private final String tenantId;
        private final Instant createdAt;
        private final Instant expiresAt;

        public UserSession(String sessionId, String userId, String tenantId, Instant createdAt, Instant expiresAt) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.tenantId = tenantId;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getTenantId() { return tenantId; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getExpiresAt() { return expiresAt; }
    }

    private static class AuthenticationState {
        private final String tenantId;
        private final String redirectUri;
        private final String state;
        private final String nonce;
        private final Instant expiresAt;

        public AuthenticationState(String tenantId, String redirectUri, String state, String nonce, Instant expiresAt) {
            this.tenantId = tenantId;
            this.redirectUri = redirectUri;
            this.state = state;
            this.nonce = nonce;
            this.expiresAt = expiresAt;
        }

        // Getters
        public String getTenantId() { return tenantId; }
        public String getRedirectUri() { return redirectUri; }
        public String getState() { return state; }
        public String getNonce() { return nonce; }
        public Instant getExpiresAt() { return expiresAt; }
    }

    private static class TokenResponse {
        public final String accessToken;
        public final String idToken;
        public final String refreshToken;
        public final int expiresIn;

        public TokenResponse(String accessToken, String idToken, String refreshToken, int expiresIn) {
            this.accessToken = accessToken;
            this.idToken = idToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }
    }

    private static class UserInfo {
        private final String id;
        private final String username;
        private final String email;
        private final String firstName;
        private final String lastName;
        private final List<String> roles;
        private final List<String> permissions;

        public UserInfo(String id, String username, String email, String firstName,
                       String lastName, List<String> roles, List<String> permissions) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.roles = roles;
            this.permissions = permissions;
        }

        // Getters
        public String getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public List<String> getRoles() { return roles; }
        public List<String> getPermissions() { return permissions; }
    }

    public static class AuthenticationStatistics {
        public final int activeStates;
        public final int activeSessions;
        public final boolean ssoEnabled;
        public final String provider;

        public AuthenticationStatistics(int activeStates, int activeSessions, boolean ssoEnabled, String provider) {
            this.activeStates = activeStates;
            this.activeSessions = activeSessions;
            this.ssoEnabled = ssoEnabled;
            this.provider = provider;
        }
    }
}
