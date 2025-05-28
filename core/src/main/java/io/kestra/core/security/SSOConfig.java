package io.kestra.core.security;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Introspected;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration properties for SSO/OIDC authentication
 */
@ConfigurationProperties("kestra.security.sso")
@Data
@Introspected
public class SSOConfig {
    
    /**
     * Enable or disable SSO authentication
     */
    private boolean enabled = false;
    
    /**
     * SSO provider type
     */
    @NotNull
    private ProviderType provider = ProviderType.OIDC;
    
    /**
     * OIDC configuration
     */
    private OIDCConfig oidc = new OIDCConfig();
    
    /**
     * SAML configuration
     */
    private SAMLConfig saml = new SAMLConfig();
    
    /**
     * LDAP configuration
     */
    private LDAPConfig ldap = new LDAPConfig();
    
    /**
     * OAuth2 configuration
     */
    private OAuth2Config oauth2 = new OAuth2Config();
    
    /**
     * Session management configuration
     */
    private SessionConfig session = new SessionConfig();
    
    /**
     * User mapping configuration
     */
    private UserMappingConfig userMapping = new UserMappingConfig();
    
    /**
     * Multi-factor authentication configuration
     */
    private MFAConfig mfa = new MFAConfig();
    
    /**
     * SSO provider types
     */
    public enum ProviderType {
        OIDC,
        SAML,
        LDAP,
        OAUTH2,
        CUSTOM
    }
    
    /**
     * OIDC configuration
     */
    @Data
    public static class OIDCConfig {
        /**
         * OIDC issuer URL
         */
        @NotBlank
        private String issuer;
        
        /**
         * Client ID
         */
        @NotBlank
        private String clientId;
        
        /**
         * Client secret
         */
        @NotBlank
        private String clientSecret;
        
        /**
         * Redirect URI
         */
        @NotBlank
        private String redirectUri;
        
        /**
         * Scopes to request
         */
        private Set<String> scopes = Set.of("openid", "profile", "email");
        
        /**
         * Additional authorization parameters
         */
        private Map<String, String> authorizationParams = Map.of();
        
        /**
         * Token endpoint authentication method
         */
        private String tokenEndpointAuthMethod = "client_secret_basic";
        
        /**
         * JWKS cache duration
         */
        private Duration jwksCacheDuration = Duration.ofMinutes(5);
        
        /**
         * Connect timeout
         */
        private Duration connectTimeout = Duration.ofSeconds(10);
        
        /**
         * Read timeout
         */
        private Duration readTimeout = Duration.ofSeconds(10);
    }
    
    /**
     * SAML configuration
     */
    @Data
    public static class SAMLConfig {
        /**
         * Identity Provider metadata URL
         */
        private String idpMetadataUrl;
        
        /**
         * Identity Provider metadata XML
         */
        private String idpMetadataXml;
        
        /**
         * Service Provider entity ID
         */
        @NotBlank
        private String spEntityId;
        
        /**
         * Assertion Consumer Service URL
         */
        @NotBlank
        private String acsUrl;
        
        /**
         * Single Logout Service URL
         */
        private String sloUrl;
        
        /**
         * Certificate for signature verification
         */
        private String certificate;
        
        /**
         * Private key for signing
         */
        private String privateKey;
        
        /**
         * Sign authentication requests
         */
        private boolean signRequests = false;
        
        /**
         * Require signed assertions
         */
        private boolean requireSignedAssertions = true;
        
        /**
         * Assertion validity duration
         */
        private Duration assertionValidityDuration = Duration.ofMinutes(5);
    }
    
    /**
     * LDAP configuration
     */
    @Data
    public static class LDAPConfig {
        /**
         * LDAP server URL
         */
        @NotBlank
        private String url;
        
        /**
         * Base DN for user searches
         */
        @NotBlank
        private String baseDn;
        
        /**
         * User search filter
         */
        private String userSearchFilter = "(uid={0})";
        
        /**
         * Group search base
         */
        private String groupSearchBase;
        
        /**
         * Group search filter
         */
        private String groupSearchFilter = "(member={0})";
        
        /**
         * Bind DN for authentication
         */
        private String bindDn;
        
        /**
         * Bind password
         */
        private String bindPassword;
        
        /**
         * Connection timeout
         */
        private Duration connectTimeout = Duration.ofSeconds(5);
        
        /**
         * Read timeout
         */
        private Duration readTimeout = Duration.ofSeconds(10);
        
        /**
         * Use SSL/TLS
         */
        private boolean useSsl = false;
        
        /**
         * Trust all certificates (for testing only)
         */
        private boolean trustAllCertificates = false;
    }
    
    /**
     * OAuth2 configuration
     */
    @Data
    public static class OAuth2Config {
        /**
         * Authorization endpoint
         */
        @NotBlank
        private String authorizationUri;
        
        /**
         * Token endpoint
         */
        @NotBlank
        private String tokenUri;
        
        /**
         * User info endpoint
         */
        private String userInfoUri;
        
        /**
         * Client ID
         */
        @NotBlank
        private String clientId;
        
        /**
         * Client secret
         */
        @NotBlank
        private String clientSecret;
        
        /**
         * Scopes to request
         */
        private Set<String> scopes = Set.of("read:user", "user:email");
        
        /**
         * Grant type
         */
        private String grantType = "authorization_code";
        
        /**
         * Response type
         */
        private String responseType = "code";
    }
    
    /**
     * Session management configuration
     */
    @Data
    public static class SessionConfig {
        /**
         * Session timeout duration
         */
        private Duration timeout = Duration.ofHours(8);
        
        /**
         * Remember me duration
         */
        private Duration rememberMeDuration = Duration.ofDays(30);
        
        /**
         * Maximum concurrent sessions per user
         */
        private int maxConcurrentSessions = 5;
        
        /**
         * Session fixation protection
         */
        private boolean sessionFixationProtection = true;
        
        /**
         * Secure cookies
         */
        private boolean secureCookies = true;
        
        /**
         * HTTP-only cookies
         */
        private boolean httpOnlyCookies = true;
        
        /**
         * SameSite cookie attribute
         */
        private String sameSite = "Strict";
    }
    
    /**
     * User mapping configuration
     */
    @Data
    public static class UserMappingConfig {
        /**
         * Username attribute mapping
         */
        private String usernameAttribute = "preferred_username";
        
        /**
         * Email attribute mapping
         */
        private String emailAttribute = "email";
        
        /**
         * First name attribute mapping
         */
        private String firstNameAttribute = "given_name";
        
        /**
         * Last name attribute mapping
         */
        private String lastNameAttribute = "family_name";
        
        /**
         * Groups attribute mapping
         */
        private String groupsAttribute = "groups";
        
        /**
         * Roles attribute mapping
         */
        private String rolesAttribute = "roles";
        
        /**
         * Default roles for new users
         */
        private Set<String> defaultRoles = Set.of("USER");
        
        /**
         * Auto-create users on first login
         */
        private boolean autoCreateUsers = true;
        
        /**
         * Auto-update user information
         */
        private boolean autoUpdateUsers = true;
        
        /**
         * Role mapping from external groups to internal roles
         */
        private Map<String, String> roleMapping = Map.of();
    }
    
    /**
     * Multi-factor authentication configuration
     */
    @Data
    public static class MFAConfig {
        /**
         * Enable MFA
         */
        private boolean enabled = false;
        
        /**
         * MFA providers
         */
        private Set<MFAProvider> providers = Set.of(MFAProvider.TOTP);
        
        /**
         * Require MFA for all users
         */
        private boolean required = false;
        
        /**
         * MFA bypass for trusted networks
         */
        private Set<String> trustedNetworks = Set.of();
        
        /**
         * TOTP configuration
         */
        private TOTPConfig totp = new TOTPConfig();
        
        /**
         * SMS configuration
         */
        private SMSConfig sms = new SMSConfig();
        
        /**
         * Email configuration
         */
        private EmailConfig email = new EmailConfig();
        
        public enum MFAProvider {
            TOTP,
            SMS,
            EMAIL,
            HARDWARE_TOKEN
        }
        
        @Data
        public static class TOTPConfig {
            /**
             * Issuer name
             */
            private String issuer = "DataFlare";
            
            /**
             * Time step in seconds
             */
            private int timeStep = 30;
            
            /**
             * Code digits
             */
            private int codeDigits = 6;
            
            /**
             * Algorithm
             */
            private String algorithm = "SHA1";
        }
        
        @Data
        public static class SMSConfig {
            /**
             * SMS provider
             */
            private String provider;
            
            /**
             * API key
             */
            private String apiKey;
            
            /**
             * From number
             */
            private String fromNumber;
        }
        
        @Data
        public static class EmailConfig {
            /**
             * Email provider
             */
            private String provider;
            
            /**
             * From address
             */
            private String fromAddress;
            
            /**
             * Subject template
             */
            private String subjectTemplate = "DataFlare MFA Code";
        }
    }
    
    /**
     * Get effective provider configuration
     */
    public Object getProviderConfig() {
        return switch (provider) {
            case OIDC -> oidc;
            case SAML -> saml;
            case LDAP -> ldap;
            case OAUTH2 -> oauth2;
            case CUSTOM -> null;
        };
    }
    
    /**
     * Check if MFA is enabled and required
     */
    public boolean isMFARequired() {
        return mfa.enabled && mfa.required;
    }
    
    /**
     * Check if auto user creation is enabled
     */
    public boolean isAutoCreateUsers() {
        return userMapping.autoCreateUsers;
    }
    
    /**
     * Get mapped role for external group
     */
    public String getMappedRole(String externalGroup) {
        return userMapping.roleMapping.getOrDefault(externalGroup, externalGroup);
    }
}
