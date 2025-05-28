package io.kestra.core.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SSOConfigTest {
    
    private SSOConfig ssoConfig;
    
    @BeforeEach
    void setUp() {
        ssoConfig = new SSOConfig();
    }
    
    @Test
    void testDefaultConfiguration() {
        assertFalse(ssoConfig.isEnabled());
        assertEquals(SSOConfig.ProviderType.OIDC, ssoConfig.getProvider());
        assertNotNull(ssoConfig.getOidc());
        assertNotNull(ssoConfig.getSaml());
        assertNotNull(ssoConfig.getLdap());
        assertNotNull(ssoConfig.getOauth2());
        assertNotNull(ssoConfig.getSession());
        assertNotNull(ssoConfig.getUserMapping());
        assertNotNull(ssoConfig.getMfa());
    }
    
    @Test
    void testOIDCConfiguration() {
        SSOConfig.OIDCConfig oidc = ssoConfig.getOidc();
        
        // Test default values
        assertEquals(Set.of("openid", "profile", "email"), oidc.getScopes());
        assertEquals("client_secret_basic", oidc.getTokenEndpointAuthMethod());
        assertEquals(Duration.ofMinutes(5), oidc.getJwksCacheDuration());
        assertEquals(Duration.ofSeconds(10), oidc.getConnectTimeout());
        assertEquals(Duration.ofSeconds(10), oidc.getReadTimeout());
        assertEquals(Map.of(), oidc.getAuthorizationParams());
        
        // Test setters
        oidc.setIssuer("https://auth.example.com");
        oidc.setClientId("test-client");
        oidc.setClientSecret("test-secret");
        oidc.setRedirectUri("https://app.example.com/callback");
        
        assertEquals("https://auth.example.com", oidc.getIssuer());
        assertEquals("test-client", oidc.getClientId());
        assertEquals("test-secret", oidc.getClientSecret());
        assertEquals("https://app.example.com/callback", oidc.getRedirectUri());
    }
    
    @Test
    void testSAMLConfiguration() {
        SSOConfig.SAMLConfig saml = ssoConfig.getSaml();
        
        // Test default values
        assertFalse(saml.isSignRequests());
        assertTrue(saml.isRequireSignedAssertions());
        assertEquals(Duration.ofMinutes(5), saml.getAssertionValidityDuration());
        
        // Test setters
        saml.setIdpMetadataUrl("https://idp.example.com/metadata");
        saml.setSpEntityId("dataflare-sp");
        saml.setAcsUrl("https://app.example.com/saml/acs");
        saml.setSloUrl("https://app.example.com/saml/slo");
        saml.setSignRequests(true);
        saml.setRequireSignedAssertions(false);
        
        assertEquals("https://idp.example.com/metadata", saml.getIdpMetadataUrl());
        assertEquals("dataflare-sp", saml.getSpEntityId());
        assertEquals("https://app.example.com/saml/acs", saml.getAcsUrl());
        assertEquals("https://app.example.com/saml/slo", saml.getSloUrl());
        assertTrue(saml.isSignRequests());
        assertFalse(saml.isRequireSignedAssertions());
    }
    
    @Test
    void testLDAPConfiguration() {
        SSOConfig.LDAPConfig ldap = ssoConfig.getLdap();
        
        // Test default values
        assertEquals("(uid={0})", ldap.getUserSearchFilter());
        assertEquals("(member={0})", ldap.getGroupSearchFilter());
        assertEquals(Duration.ofSeconds(5), ldap.getConnectTimeout());
        assertEquals(Duration.ofSeconds(10), ldap.getReadTimeout());
        assertFalse(ldap.isUseSsl());
        assertFalse(ldap.isTrustAllCertificates());
        
        // Test setters
        ldap.setUrl("ldap://ldap.example.com:389");
        ldap.setBaseDn("ou=users,dc=example,dc=com");
        ldap.setUserSearchFilter("(sAMAccountName={0})");
        ldap.setGroupSearchBase("ou=groups,dc=example,dc=com");
        ldap.setBindDn("cn=admin,dc=example,dc=com");
        ldap.setBindPassword("admin-password");
        ldap.setUseSsl(true);
        
        assertEquals("ldap://ldap.example.com:389", ldap.getUrl());
        assertEquals("ou=users,dc=example,dc=com", ldap.getBaseDn());
        assertEquals("(sAMAccountName={0})", ldap.getUserSearchFilter());
        assertEquals("ou=groups,dc=example,dc=com", ldap.getGroupSearchBase());
        assertEquals("cn=admin,dc=example,dc=com", ldap.getBindDn());
        assertEquals("admin-password", ldap.getBindPassword());
        assertTrue(ldap.isUseSsl());
    }
    
    @Test
    void testOAuth2Configuration() {
        SSOConfig.OAuth2Config oauth2 = ssoConfig.getOauth2();
        
        // Test default values
        assertEquals(Set.of("read:user", "user:email"), oauth2.getScopes());
        assertEquals("authorization_code", oauth2.getGrantType());
        assertEquals("code", oauth2.getResponseType());
        
        // Test setters
        oauth2.setAuthorizationUri("https://github.com/login/oauth/authorize");
        oauth2.setTokenUri("https://github.com/login/oauth/access_token");
        oauth2.setUserInfoUri("https://api.github.com/user");
        oauth2.setClientId("github-client-id");
        oauth2.setClientSecret("github-client-secret");
        oauth2.setScopes(Set.of("user", "repo"));
        
        assertEquals("https://github.com/login/oauth/authorize", oauth2.getAuthorizationUri());
        assertEquals("https://github.com/login/oauth/access_token", oauth2.getTokenUri());
        assertEquals("https://api.github.com/user", oauth2.getUserInfoUri());
        assertEquals("github-client-id", oauth2.getClientId());
        assertEquals("github-client-secret", oauth2.getClientSecret());
        assertEquals(Set.of("user", "repo"), oauth2.getScopes());
    }
    
    @Test
    void testSessionConfiguration() {
        SSOConfig.SessionConfig session = ssoConfig.getSession();
        
        // Test default values
        assertEquals(Duration.ofHours(8), session.getTimeout());
        assertEquals(Duration.ofDays(30), session.getRememberMeDuration());
        assertEquals(5, session.getMaxConcurrentSessions());
        assertTrue(session.isSessionFixationProtection());
        assertTrue(session.isSecureCookies());
        assertTrue(session.isHttpOnlyCookies());
        assertEquals("Strict", session.getSameSite());
        
        // Test setters
        session.setTimeout(Duration.ofHours(4));
        session.setRememberMeDuration(Duration.ofDays(7));
        session.setMaxConcurrentSessions(3);
        session.setSessionFixationProtection(false);
        session.setSecureCookies(false);
        session.setHttpOnlyCookies(false);
        session.setSameSite("Lax");
        
        assertEquals(Duration.ofHours(4), session.getTimeout());
        assertEquals(Duration.ofDays(7), session.getRememberMeDuration());
        assertEquals(3, session.getMaxConcurrentSessions());
        assertFalse(session.isSessionFixationProtection());
        assertFalse(session.isSecureCookies());
        assertFalse(session.isHttpOnlyCookies());
        assertEquals("Lax", session.getSameSite());
    }
    
    @Test
    void testUserMappingConfiguration() {
        SSOConfig.UserMappingConfig userMapping = ssoConfig.getUserMapping();
        
        // Test default values
        assertEquals("preferred_username", userMapping.getUsernameAttribute());
        assertEquals("email", userMapping.getEmailAttribute());
        assertEquals("given_name", userMapping.getFirstNameAttribute());
        assertEquals("family_name", userMapping.getLastNameAttribute());
        assertEquals("groups", userMapping.getGroupsAttribute());
        assertEquals("roles", userMapping.getRolesAttribute());
        assertEquals(Set.of("USER"), userMapping.getDefaultRoles());
        assertTrue(userMapping.isAutoCreateUsers());
        assertTrue(userMapping.isAutoUpdateUsers());
        assertEquals(Map.of(), userMapping.getRoleMapping());
        
        // Test setters
        userMapping.setUsernameAttribute("sub");
        userMapping.setEmailAttribute("email_address");
        userMapping.setFirstNameAttribute("first_name");
        userMapping.setLastNameAttribute("last_name");
        userMapping.setGroupsAttribute("user_groups");
        userMapping.setRolesAttribute("user_roles");
        userMapping.setDefaultRoles(Set.of("VIEWER", "USER"));
        userMapping.setAutoCreateUsers(false);
        userMapping.setAutoUpdateUsers(false);
        userMapping.setRoleMapping(Map.of("admin", "ADMIN", "user", "USER"));
        
        assertEquals("sub", userMapping.getUsernameAttribute());
        assertEquals("email_address", userMapping.getEmailAttribute());
        assertEquals("first_name", userMapping.getFirstNameAttribute());
        assertEquals("last_name", userMapping.getLastNameAttribute());
        assertEquals("user_groups", userMapping.getGroupsAttribute());
        assertEquals("user_roles", userMapping.getRolesAttribute());
        assertEquals(Set.of("VIEWER", "USER"), userMapping.getDefaultRoles());
        assertFalse(userMapping.isAutoCreateUsers());
        assertFalse(userMapping.isAutoUpdateUsers());
        assertEquals(Map.of("admin", "ADMIN", "user", "USER"), userMapping.getRoleMapping());
    }
    
    @Test
    void testMFAConfiguration() {
        SSOConfig.MFAConfig mfa = ssoConfig.getMfa();
        
        // Test default values
        assertFalse(mfa.isEnabled());
        assertEquals(Set.of(SSOConfig.MFAConfig.MFAProvider.TOTP), mfa.getProviders());
        assertFalse(mfa.isRequired());
        assertEquals(Set.of(), mfa.getTrustedNetworks());
        assertNotNull(mfa.getTotp());
        assertNotNull(mfa.getSms());
        assertNotNull(mfa.getEmail());
        
        // Test setters
        mfa.setEnabled(true);
        mfa.setRequired(true);
        mfa.setProviders(Set.of(SSOConfig.MFAConfig.MFAProvider.TOTP, SSOConfig.MFAConfig.MFAProvider.SMS));
        mfa.setTrustedNetworks(Set.of("192.168.1.0/24", "10.0.0.0/8"));
        
        assertTrue(mfa.isEnabled());
        assertTrue(mfa.isRequired());
        assertEquals(Set.of(SSOConfig.MFAConfig.MFAProvider.TOTP, SSOConfig.MFAConfig.MFAProvider.SMS), mfa.getProviders());
        assertEquals(Set.of("192.168.1.0/24", "10.0.0.0/8"), mfa.getTrustedNetworks());
    }
    
    @Test
    void testTOTPConfiguration() {
        SSOConfig.MFAConfig.TOTPConfig totp = ssoConfig.getMfa().getTotp();
        
        // Test default values
        assertEquals("DataFlare", totp.getIssuer());
        assertEquals(30, totp.getTimeStep());
        assertEquals(6, totp.getCodeDigits());
        assertEquals("SHA1", totp.getAlgorithm());
        
        // Test setters
        totp.setIssuer("MyApp");
        totp.setTimeStep(60);
        totp.setCodeDigits(8);
        totp.setAlgorithm("SHA256");
        
        assertEquals("MyApp", totp.getIssuer());
        assertEquals(60, totp.getTimeStep());
        assertEquals(8, totp.getCodeDigits());
        assertEquals("SHA256", totp.getAlgorithm());
    }
    
    @Test
    void testProviderTypeConfiguration() {
        // Test all provider types
        for (SSOConfig.ProviderType providerType : SSOConfig.ProviderType.values()) {
            ssoConfig.setProvider(providerType);
            assertEquals(providerType, ssoConfig.getProvider());
        }
    }
    
    @Test
    void testGetProviderConfig() {
        // Test OIDC provider config
        ssoConfig.setProvider(SSOConfig.ProviderType.OIDC);
        assertEquals(ssoConfig.getOidc(), ssoConfig.getProviderConfig());
        
        // Test SAML provider config
        ssoConfig.setProvider(SSOConfig.ProviderType.SAML);
        assertEquals(ssoConfig.getSaml(), ssoConfig.getProviderConfig());
        
        // Test LDAP provider config
        ssoConfig.setProvider(SSOConfig.ProviderType.LDAP);
        assertEquals(ssoConfig.getLdap(), ssoConfig.getProviderConfig());
        
        // Test OAuth2 provider config
        ssoConfig.setProvider(SSOConfig.ProviderType.OAUTH2);
        assertEquals(ssoConfig.getOauth2(), ssoConfig.getProviderConfig());
        
        // Test custom provider config
        ssoConfig.setProvider(SSOConfig.ProviderType.CUSTOM);
        assertNull(ssoConfig.getProviderConfig());
    }
    
    @Test
    void testMFARequiredCheck() {
        // Test when MFA is disabled
        assertFalse(ssoConfig.isMFARequired());
        
        // Test when MFA is enabled but not required
        ssoConfig.getMfa().setEnabled(true);
        assertFalse(ssoConfig.isMFARequired());
        
        // Test when MFA is enabled and required
        ssoConfig.getMfa().setRequired(true);
        assertTrue(ssoConfig.isMFARequired());
    }
    
    @Test
    void testAutoCreateUsersCheck() {
        // Test default value
        assertTrue(ssoConfig.isAutoCreateUsers());
        
        // Test when disabled
        ssoConfig.getUserMapping().setAutoCreateUsers(false);
        assertFalse(ssoConfig.isAutoCreateUsers());
    }
    
    @Test
    void testRoleMapping() {
        // Test default mapping (no mapping)
        assertEquals("admin", ssoConfig.getMappedRole("admin"));
        assertEquals("user", ssoConfig.getMappedRole("user"));
        
        // Test with custom mapping
        ssoConfig.getUserMapping().setRoleMapping(Map.of(
            "administrators", "ADMIN",
            "users", "USER",
            "viewers", "VIEWER"
        ));
        
        assertEquals("ADMIN", ssoConfig.getMappedRole("administrators"));
        assertEquals("USER", ssoConfig.getMappedRole("users"));
        assertEquals("VIEWER", ssoConfig.getMappedRole("viewers"));
        assertEquals("unknown", ssoConfig.getMappedRole("unknown")); // No mapping
    }
    
    @Test
    void testConfigurationModification() {
        // Test enabling SSO
        ssoConfig.setEnabled(true);
        assertTrue(ssoConfig.isEnabled());
        
        // Test changing provider
        ssoConfig.setProvider(SSOConfig.ProviderType.SAML);
        assertEquals(SSOConfig.ProviderType.SAML, ssoConfig.getProvider());
        
        // Test modifying nested configurations
        ssoConfig.getOidc().setIssuer("https://new-issuer.com");
        assertEquals("https://new-issuer.com", ssoConfig.getOidc().getIssuer());
        
        ssoConfig.getMfa().setEnabled(true);
        assertTrue(ssoConfig.getMfa().isEnabled());
        
        ssoConfig.getSession().setTimeout(Duration.ofHours(12));
        assertEquals(Duration.ofHours(12), ssoConfig.getSession().getTimeout());
    }
}
