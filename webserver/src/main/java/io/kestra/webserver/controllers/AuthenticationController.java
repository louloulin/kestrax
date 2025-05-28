package io.kestra.webserver.controllers;

import io.kestra.core.models.rbac.User;
import io.kestra.core.security.SSOConfig;
import io.kestra.core.services.JWTTokenService;
import io.kestra.core.services.MFAService;
import io.kestra.core.services.SSOAuthenticationService;
import io.kestra.core.services.UserService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller for authentication and SSO operations
 */
@Controller("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and SSO management")
@Slf4j
public class AuthenticationController {
    
    private final SSOConfig ssoConfig;
    private final SSOAuthenticationService ssoAuthenticationService;
    private final JWTTokenService jwtTokenService;
    private final MFAService mfaService;
    private final UserService userService;
    
    @Inject
    public AuthenticationController(
        SSOConfig ssoConfig,
        SSOAuthenticationService ssoAuthenticationService,
        JWTTokenService jwtTokenService,
        MFAService mfaService,
        UserService userService
    ) {
        this.ssoConfig = ssoConfig;
        this.ssoAuthenticationService = ssoAuthenticationService;
        this.jwtTokenService = jwtTokenService;
        this.mfaService = mfaService;
        this.userService = userService;
    }
    
    /**
     * Get SSO configuration
     */
    @Get("/sso/config")
    @Operation(summary = "Get SSO configuration", description = "Retrieve SSO configuration for client")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<SSOConfigResponse> getSSOConfig() {
        SSOConfigResponse response = new SSOConfigResponse(
            ssoConfig.isEnabled(),
            ssoConfig.getProvider().name(),
            ssoConfig.getMfa().isEnabled(),
            ssoConfig.getMfa().isRequired(),
            ssoConfig.getMfa().getProviders()
        );
        
        return HttpResponse.ok(response);
    }
    
    /**
     * Initiate SSO authentication
     */
    @Post("/sso/initiate")
    @Operation(summary = "Initiate SSO authentication", description = "Start SSO authentication flow")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<AuthInitiationResponse> initiateSSOAuth(@Body @Valid AuthInitiationRequest request) {
        try {
            SSOAuthenticationService.AuthenticationInitiation initiation = 
                ssoAuthenticationService.initiateAuthentication(request.tenantId, request.redirectUri);
            
            AuthInitiationResponse response = new AuthInitiationResponse(
                initiation.authorizationUrl,
                initiation.state
            );
            
            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Failed to initiate SSO authentication", e);
            return HttpResponse.badRequest();
        }
    }
    
    /**
     * Handle SSO callback
     */
    @Post("/sso/callback")
    @Operation(summary = "Handle SSO callback", description = "Process SSO authentication callback")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<AuthCallbackResponse> handleSSOCallback(@Body @Valid AuthCallbackRequest request) {
        try {
            SSOAuthenticationService.AuthenticationResult result = 
                ssoAuthenticationService.handleCallback(request.code, request.state, request.error);
            
            if (!result.isSuccess()) {
                return HttpResponse.badRequest(AuthCallbackResponse.error(result.getError()));
            }
            
            // Check if MFA is required
            if (mfaService.isMFARequired(result.getUser(), request.clientIP)) {
                // Return MFA challenge
                MFAService.MFAChallengeResult mfaChallenge = mfaService.initiateMFAChallenge(
                    result.getUser().getId(), 
                    SSOConfig.MFAConfig.MFAProvider.TOTP
                );
                
                if (mfaChallenge.isSuccess()) {
                    return HttpResponse.ok(AuthCallbackResponse.mfaRequired(
                        mfaChallenge.getChallengeId(),
                        mfaChallenge.getMessage()
                    ));
                } else {
                    return HttpResponse.badRequest(AuthCallbackResponse.error(mfaChallenge.getError()));
                }
            }
            
            // Authentication successful
            AuthCallbackResponse response = AuthCallbackResponse.success(
                result.getAccessToken(),
                result.getRefreshToken(),
                result.getSessionId(),
                result.getUser(),
                result.getRedirectUri()
            );
            
            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Failed to handle SSO callback", e);
            return HttpResponse.badRequest(AuthCallbackResponse.error("Authentication failed"));
        }
    }
    
    /**
     * Verify MFA challenge
     */
    @Post("/mfa/verify")
    @Operation(summary = "Verify MFA challenge", description = "Verify MFA challenge code")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<MFAVerificationResponse> verifyMFA(@Body @Valid MFAVerificationRequest request) {
        try {
            MFAService.MFAVerificationResult result = mfaService.verifyMFAChallenge(
                request.challengeId, 
                request.code
            );
            
            if (result.isSuccess()) {
                return HttpResponse.ok(MFAVerificationResponse.success());
            } else {
                return HttpResponse.badRequest(MFAVerificationResponse.failure(result.getError()));
            }
        } catch (Exception e) {
            log.error("Failed to verify MFA", e);
            return HttpResponse.badRequest(MFAVerificationResponse.failure("MFA verification failed"));
        }
    }
    
    /**
     * Refresh access token
     */
    @Post("/token/refresh")
    @Operation(summary = "Refresh access token", description = "Refresh access token using refresh token")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<TokenRefreshResponse> refreshToken(@Body @Valid TokenRefreshRequest request) {
        try {
            JWTTokenService.TokenRefreshResult result = jwtTokenService.refreshAccessToken(request.refreshToken);
            
            if (result.isSuccess()) {
                TokenRefreshResponse response = new TokenRefreshResponse(
                    result.getAccessToken(),
                    result.getRefreshToken()
                );
                return HttpResponse.ok(response);
            } else {
                return HttpResponse.unauthorized();
            }
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            return HttpResponse.unauthorized();
        }
    }
    
    /**
     * Logout user
     */
    @Post("/logout")
    @Operation(summary = "Logout user", description = "Logout user and revoke tokens")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<LogoutResponse> logout(@Body @Valid LogoutRequest request) {
        try {
            SSOAuthenticationService.LogoutResult result = ssoAuthenticationService.logout(
                request.sessionId, 
                request.accessToken
            );
            
            LogoutResponse response = new LogoutResponse(result.success, result.logoutUrl);
            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Failed to logout", e);
            return HttpResponse.badRequest();
        }
    }
    
    /**
     * Get user MFA status
     */
    @Get("/mfa/status/{userId}")
    @Operation(summary = "Get user MFA status", description = "Get MFA status for user")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<MFAStatusResponse> getMFAStatus(@PathVariable String userId) {
        try {
            MFAService.MFAStatus status = mfaService.getUserMFAStatus(userId);
            
            MFAStatusResponse response = new MFAStatusResponse(
                status.enabled,
                status.enabledProviders,
                status.backupCodesCount,
                status.setupPending
            );
            
            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Failed to get MFA status", e);
            return HttpResponse.badRequest();
        }
    }
    
    /**
     * Setup TOTP for user
     */
    @Post("/mfa/totp/setup/{userId}")
    @Operation(summary = "Setup TOTP", description = "Setup TOTP for user")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<TOTPSetupResponse> setupTOTP(@PathVariable String userId) {
        try {
            MFAService.TOTPSetupResult result = mfaService.setupTOTP(userId);
            
            TOTPSetupResponse response = new TOTPSetupResponse(
                result.secret,
                result.qrCodeData
            );
            
            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Failed to setup TOTP", e);
            return HttpResponse.badRequest();
        }
    }
    
    /**
     * Confirm TOTP setup
     */
    @Post("/mfa/totp/confirm/{userId}")
    @Operation(summary = "Confirm TOTP setup", description = "Confirm TOTP setup with verification code")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<TOTPConfirmResponse> confirmTOTP(
        @PathVariable String userId, 
        @Body @Valid TOTPConfirmRequest request
    ) {
        try {
            boolean success = mfaService.confirmTOTPSetup(userId, request.code);
            
            TOTPConfirmResponse response = new TOTPConfirmResponse(success);
            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Failed to confirm TOTP setup", e);
            return HttpResponse.badRequest();
        }
    }
    
    /**
     * Disable MFA for user
     */
    @Delete("/mfa/{userId}")
    @Operation(summary = "Disable MFA", description = "Disable MFA for user")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<MFADisableResponse> disableMFA(@PathVariable String userId) {
        try {
            boolean success = mfaService.disableMFA(userId);
            
            MFADisableResponse response = new MFADisableResponse(success);
            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Failed to disable MFA", e);
            return HttpResponse.badRequest();
        }
    }
    
    /**
     * Get authentication statistics
     */
    @Get("/statistics")
    @Operation(summary = "Get authentication statistics", description = "Get authentication and MFA statistics")
    @Secured("ADMIN")
    public HttpResponse<AuthStatisticsResponse> getStatistics() {
        try {
            SSOAuthenticationService.AuthenticationStatistics authStats = 
                ssoAuthenticationService.getStatistics();
            MFAService.MFAStatistics mfaStats = mfaService.getStatistics();
            JWTTokenService.TokenStatistics tokenStats = jwtTokenService.getTokenStatistics();
            
            AuthStatisticsResponse response = new AuthStatisticsResponse(
                authStats,
                mfaStats,
                tokenStats
            );
            
            return HttpResponse.ok(response);
        } catch (Exception e) {
            log.error("Failed to get authentication statistics", e);
            return HttpResponse.badRequest();
        }
    }
    
    // Request/Response DTOs
    
    public static class AuthInitiationRequest {
        @NotBlank
        public String tenantId;
        
        @NotBlank
        public String redirectUri;
    }
    
    public static class AuthInitiationResponse {
        public final String authorizationUrl;
        public final String state;
        
        public AuthInitiationResponse(String authorizationUrl, String state) {
            this.authorizationUrl = authorizationUrl;
            this.state = state;
        }
    }
    
    public static class AuthCallbackRequest {
        public String code;
        public String state;
        public String error;
        public String clientIP;
    }
    
    public static class AuthCallbackResponse {
        public final boolean success;
        public final String error;
        public final String accessToken;
        public final String refreshToken;
        public final String sessionId;
        public final User user;
        public final String redirectUri;
        public final boolean mfaRequired;
        public final String mfaChallengeId;
        public final String mfaMessage;
        
        private AuthCallbackResponse(boolean success, String error, String accessToken, 
                                   String refreshToken, String sessionId, User user, 
                                   String redirectUri, boolean mfaRequired, 
                                   String mfaChallengeId, String mfaMessage) {
            this.success = success;
            this.error = error;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.sessionId = sessionId;
            this.user = user;
            this.redirectUri = redirectUri;
            this.mfaRequired = mfaRequired;
            this.mfaChallengeId = mfaChallengeId;
            this.mfaMessage = mfaMessage;
        }
        
        public static AuthCallbackResponse success(String accessToken, String refreshToken, 
                                                 String sessionId, User user, String redirectUri) {
            return new AuthCallbackResponse(true, null, accessToken, refreshToken, sessionId, 
                                          user, redirectUri, false, null, null);
        }
        
        public static AuthCallbackResponse mfaRequired(String challengeId, String message) {
            return new AuthCallbackResponse(true, null, null, null, null, null, null, 
                                          true, challengeId, message);
        }
        
        public static AuthCallbackResponse error(String error) {
            return new AuthCallbackResponse(false, error, null, null, null, null, null, 
                                          false, null, null);
        }
    }
    
    public static class MFAVerificationRequest {
        @NotBlank
        public String challengeId;
        
        @NotBlank
        public String code;
    }
    
    public static class MFAVerificationResponse {
        public final boolean success;
        public final String error;
        
        private MFAVerificationResponse(boolean success, String error) {
            this.success = success;
            this.error = error;
        }
        
        public static MFAVerificationResponse success() {
            return new MFAVerificationResponse(true, null);
        }
        
        public static MFAVerificationResponse failure(String error) {
            return new MFAVerificationResponse(false, error);
        }
    }
    
    public static class TokenRefreshRequest {
        @NotBlank
        public String refreshToken;
    }
    
    public static class TokenRefreshResponse {
        public final String accessToken;
        public final String refreshToken;
        
        public TokenRefreshResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
    
    public static class LogoutRequest {
        public String sessionId;
        public String accessToken;
    }
    
    public static class LogoutResponse {
        public final boolean success;
        public final String logoutUrl;
        
        public LogoutResponse(boolean success, String logoutUrl) {
            this.success = success;
            this.logoutUrl = logoutUrl;
        }
    }
    
    public static class SSOConfigResponse {
        public final boolean enabled;
        public final String provider;
        public final boolean mfaEnabled;
        public final boolean mfaRequired;
        public final java.util.Set<SSOConfig.MFAConfig.MFAProvider> mfaProviders;
        
        public SSOConfigResponse(boolean enabled, String provider, boolean mfaEnabled, 
                               boolean mfaRequired, java.util.Set<SSOConfig.MFAConfig.MFAProvider> mfaProviders) {
            this.enabled = enabled;
            this.provider = provider;
            this.mfaEnabled = mfaEnabled;
            this.mfaRequired = mfaRequired;
            this.mfaProviders = mfaProviders;
        }
    }
    
    public static class MFAStatusResponse {
        public final boolean enabled;
        public final java.util.Set<SSOConfig.MFAConfig.MFAProvider> enabledProviders;
        public final int backupCodesCount;
        public final boolean setupPending;
        
        public MFAStatusResponse(boolean enabled, java.util.Set<SSOConfig.MFAConfig.MFAProvider> enabledProviders, 
                               int backupCodesCount, boolean setupPending) {
            this.enabled = enabled;
            this.enabledProviders = enabledProviders;
            this.backupCodesCount = backupCodesCount;
            this.setupPending = setupPending;
        }
    }
    
    public static class TOTPSetupResponse {
        public final String secret;
        public final String qrCodeData;
        
        public TOTPSetupResponse(String secret, String qrCodeData) {
            this.secret = secret;
            this.qrCodeData = qrCodeData;
        }
    }
    
    public static class TOTPConfirmRequest {
        @NotBlank
        public String code;
    }
    
    public static class TOTPConfirmResponse {
        public final boolean success;
        
        public TOTPConfirmResponse(boolean success) {
            this.success = success;
        }
    }
    
    public static class MFADisableResponse {
        public final boolean success;
        
        public MFADisableResponse(boolean success) {
            this.success = success;
        }
    }
    
    public static class AuthStatisticsResponse {
        public final SSOAuthenticationService.AuthenticationStatistics authStats;
        public final MFAService.MFAStatistics mfaStats;
        public final JWTTokenService.TokenStatistics tokenStats;
        
        public AuthStatisticsResponse(SSOAuthenticationService.AuthenticationStatistics authStats,
                                    MFAService.MFAStatistics mfaStats,
                                    JWTTokenService.TokenStatistics tokenStats) {
            this.authStats = authStats;
            this.mfaStats = mfaStats;
            this.tokenStats = tokenStats;
        }
    }
}
