package io.kestra.core.services;

import io.kestra.core.models.rbac.User;
import io.kestra.core.security.SSOConfig;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for Multi-Factor Authentication (MFA) management
 */
@Singleton
@Slf4j
public class MFAService {

    private final SSOConfig ssoConfig;

    // User MFA settings storage
    private final ConcurrentMap<String, UserMFASettings> userMFASettings = new ConcurrentHashMap<>();

    // Pending MFA challenges
    private final ConcurrentMap<String, MFAChallenge> pendingChallenges = new ConcurrentHashMap<>();

    // Used codes to prevent replay attacks
    private final ConcurrentMap<String, Set<String>> usedCodes = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    @Inject
    public MFAService(SSOConfig ssoConfig) {
        this.ssoConfig = ssoConfig;
    }

    /**
     * Check if MFA is required for user
     */
    public boolean isMFARequired(User user, String clientIP) {
        if (!ssoConfig.getMfa().isEnabled()) {
            return false;
        }

        // Check if MFA is globally required
        if (ssoConfig.getMfa().isRequired()) {
            return !isTrustedNetwork(clientIP);
        }

        // Check if user has MFA enabled
        UserMFASettings settings = userMFASettings.get(user.getId());
        return settings != null && settings.isEnabled();
    }

    /**
     * Setup TOTP for user
     */
    public TOTPSetupResult setupTOTP(String userId) {
        if (!ssoConfig.getMfa().getProviders().contains(SSOConfig.MFAConfig.MFAProvider.TOTP)) {
            throw new IllegalStateException("TOTP is not enabled");
        }

        // Generate secret key
        byte[] secretKey = new byte[20]; // 160 bits
        secureRandom.nextBytes(secretKey);
        String secret = Base32.encode(secretKey);

        // Generate QR code data
        SSOConfig.MFAConfig.TOTPConfig totpConfig = ssoConfig.getMfa().getTotp();
        String qrCodeData = String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=%s&digits=%d&period=%d",
            totpConfig.getIssuer(),
            userId,
            secret,
            totpConfig.getIssuer(),
            totpConfig.getAlgorithm(),
            totpConfig.getCodeDigits(),
            totpConfig.getTimeStep()
        );

        // Store temporary setup
        UserMFASettings settings = userMFASettings.computeIfAbsent(userId, k -> new UserMFASettings(userId));
        settings.setTotpSecret(secret);
        settings.setTotpSetupPending(true);

        return new TOTPSetupResult(secret, qrCodeData);
    }

    /**
     * Confirm TOTP setup
     */
    public boolean confirmTOTPSetup(String userId, String code) {
        UserMFASettings settings = userMFASettings.get(userId);
        if (settings == null || !settings.isTotpSetupPending()) {
            return false;
        }

        // Verify the provided code
        if (verifyTOTPCode(settings.getTotpSecret(), code)) {
            settings.setTotpSetupPending(false);
            settings.setEnabled(true);
            settings.getEnabledProviders().add(SSOConfig.MFAConfig.MFAProvider.TOTP);

            // Generate backup codes
            settings.setBackupCodes(generateBackupCodes());

            log.info("TOTP setup confirmed for user: {}", userId);
            return true;
        }

        return false;
    }

    /**
     * Initiate MFA challenge
     */
    public MFAChallengeResult initiateMFAChallenge(String userId, SSOConfig.MFAConfig.MFAProvider provider) {
        UserMFASettings settings = userMFASettings.get(userId);
        if (settings == null || !settings.isEnabled()) {
            return MFAChallengeResult.error("MFA not enabled for user");
        }

        if (!settings.getEnabledProviders().contains(provider)) {
            return MFAChallengeResult.error("MFA provider not enabled for user");
        }

        String challengeId = UUID.randomUUID().toString();
        MFAChallenge challenge = new MFAChallenge(
            challengeId,
            userId,
            provider,
            Instant.now().plusSeconds(300) // 5 minutes expiry
        );

        pendingChallenges.put(challengeId, challenge);

        return switch (provider) {
            case TOTP -> MFAChallengeResult.success(challengeId, "Enter TOTP code from your authenticator app");
            case SMS -> initiateSMSChallenge(challengeId, settings);
            case EMAIL -> initiateEmailChallenge(challengeId, settings);
            default -> MFAChallengeResult.error("Unsupported MFA provider");
        };
    }

    /**
     * Verify MFA challenge
     */
    public MFAVerificationResult verifyMFAChallenge(String challengeId, String code) {
        MFAChallenge challenge = pendingChallenges.remove(challengeId);
        if (challenge == null) {
            return MFAVerificationResult.failure("Invalid or expired challenge");
        }

        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            return MFAVerificationResult.failure("Challenge expired");
        }

        UserMFASettings settings = userMFASettings.get(challenge.getUserId());
        if (settings == null) {
            return MFAVerificationResult.failure("User MFA settings not found");
        }

        boolean verified = switch (challenge.getProvider()) {
            case TOTP -> verifyTOTPCode(settings.getTotpSecret(), code);
            case SMS -> verifySMSCode(challenge, code);
            case EMAIL -> verifyEmailCode(challenge, code);
            default -> false;
        };

        if (verified) {
            // Check for backup code usage
            if (settings.getBackupCodes().contains(code)) {
                settings.getBackupCodes().remove(code);
                log.info("Backup code used for user: {}", challenge.getUserId());
            }

            return MFAVerificationResult.success();
        } else {
            return MFAVerificationResult.failure("Invalid verification code");
        }
    }

    /**
     * Disable MFA for user
     */
    public boolean disableMFA(String userId) {
        UserMFASettings settings = userMFASettings.get(userId);
        if (settings != null) {
            settings.setEnabled(false);
            settings.getEnabledProviders().clear();
            settings.setTotpSecret(null);
            settings.setTotpSetupPending(false);
            settings.getBackupCodes().clear();

            log.info("MFA disabled for user: {}", userId);
            return true;
        }
        return false;
    }

    /**
     * Get user MFA status
     */
    public MFAStatus getUserMFAStatus(String userId) {
        UserMFASettings settings = userMFASettings.get(userId);
        if (settings == null) {
            return new MFAStatus(false, Set.of(), 0, false);
        }

        return new MFAStatus(
            settings.isEnabled(),
            settings.getEnabledProviders(),
            settings.getBackupCodes().size(),
            settings.isTotpSetupPending()
        );
    }

    /**
     * Generate new backup codes
     */
    public Set<String> generateNewBackupCodes(String userId) {
        UserMFASettings settings = userMFASettings.get(userId);
        if (settings == null || !settings.isEnabled()) {
            throw new IllegalStateException("MFA not enabled for user");
        }

        Set<String> newBackupCodes = generateBackupCodes();
        settings.setBackupCodes(newBackupCodes);

        log.info("New backup codes generated for user: {}", userId);
        return new HashSet<>(newBackupCodes);
    }

    /**
     * Clean up expired challenges and used codes
     */
    public void cleanupExpired() {
        Instant now = Instant.now();

        // Clean up expired challenges
        pendingChallenges.entrySet().removeIf(entry ->
            entry.getValue().getExpiresAt().isBefore(now));

        // Clean up old used codes (keep for 1 hour to prevent replay)
        Instant oneHourAgo = now.minusSeconds(3600);
        usedCodes.entrySet().removeIf(entry -> {
            // This is simplified - in practice, you'd track timestamps for each code
            return false; // Keep all for now
        });

        log.debug("Cleaned up expired MFA data. Pending challenges: {}", pendingChallenges.size());
    }

    /**
     * Get MFA statistics
     */
    public MFAStatistics getStatistics() {
        long enabledUsers = userMFASettings.values().stream()
            .mapToLong(settings -> settings.isEnabled() ? 1 : 0)
            .sum();

        return new MFAStatistics(
            ssoConfig.getMfa().isEnabled(),
            ssoConfig.getMfa().isRequired(),
            enabledUsers,
            pendingChallenges.size(),
            ssoConfig.getMfa().getProviders()
        );
    }

    /**
     * Verify TOTP code
     */
    private boolean verifyTOTPCode(String secret, String code) {
        if (secret == null || code == null) {
            return false;
        }

        try {
            byte[] secretBytes = Base32.decode(secret);
            SSOConfig.MFAConfig.TOTPConfig config = ssoConfig.getMfa().getTotp();

            long timeStep = Instant.now().getEpochSecond() / config.getTimeStep();

            // Check current time step and previous/next for clock skew
            for (int i = -1; i <= 1; i++) {
                String expectedCode = generateTOTPCode(secretBytes, timeStep + i, config);
                if (code.equals(expectedCode)) {
                    // Check if code was already used (prevent replay)
                    String codeKey = secret + ":" + code;
                    Set<String> userUsedCodes = usedCodes.computeIfAbsent(secret, k -> ConcurrentHashMap.newKeySet());

                    if (userUsedCodes.contains(code)) {
                        return false; // Code already used
                    }

                    userUsedCodes.add(code);
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("Error verifying TOTP code", e);
        }

        return false;
    }

    /**
     * Generate TOTP code
     */
    private String generateTOTPCode(byte[] secret, long timeStep, SSOConfig.MFAConfig.TOTPConfig config)
            throws NoSuchAlgorithmException, InvalidKeyException {

        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(timeStep);
        byte[] timeBytes = buffer.array();

        Mac mac = Mac.getInstance("Hmac" + config.getAlgorithm());
        SecretKeySpec keySpec = new SecretKeySpec(secret, "Hmac" + config.getAlgorithm());
        mac.init(keySpec);

        byte[] hash = mac.doFinal(timeBytes);

        int offset = hash[hash.length - 1] & 0x0F;
        int truncatedHash = ((hash[offset] & 0x7F) << 24) |
                           ((hash[offset + 1] & 0xFF) << 16) |
                           ((hash[offset + 2] & 0xFF) << 8) |
                           (hash[offset + 3] & 0xFF);

        int code = truncatedHash % (int) Math.pow(10, config.getCodeDigits());
        return String.format("%0" + config.getCodeDigits() + "d", code);
    }

    /**
     * Initiate SMS challenge
     */
    private MFAChallengeResult initiateSMSChallenge(String challengeId, UserMFASettings settings) {
        // SMS implementation would go here
        return MFAChallengeResult.error("SMS MFA not yet implemented");
    }

    /**
     * Initiate email challenge
     */
    private MFAChallengeResult initiateEmailChallenge(String challengeId, UserMFASettings settings) {
        // Email implementation would go here
        return MFAChallengeResult.error("Email MFA not yet implemented");
    }

    /**
     * Verify SMS code
     */
    private boolean verifySMSCode(MFAChallenge challenge, String code) {
        // SMS verification would go here
        return false;
    }

    /**
     * Verify email code
     */
    private boolean verifyEmailCode(MFAChallenge challenge, String code) {
        // Email verification would go here
        return false;
    }

    /**
     * Check if IP is in trusted network
     */
    private boolean isTrustedNetwork(String clientIP) {
        Set<String> trustedNetworks = ssoConfig.getMfa().getTrustedNetworks();
        return trustedNetworks.contains(clientIP) || trustedNetworks.contains("0.0.0.0/0");
    }

    /**
     * Generate backup codes
     */
    private Set<String> generateBackupCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            codes.add(generateRandomCode(8));
        }
        return codes;
    }

    /**
     * Generate random code
     */
    private String generateRandomCode(int length) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(secureRandom.nextInt(10));
        }
        return code.toString();
    }

    // Inner classes and data structures

    private static class UserMFASettings {
        private final String userId;
        private boolean enabled = false;
        private Set<SSOConfig.MFAConfig.MFAProvider> enabledProviders = new HashSet<>();
        private String totpSecret;
        private boolean totpSetupPending = false;
        private Set<String> backupCodes = new HashSet<>();

        public UserMFASettings(String userId) {
            this.userId = userId;
        }

        // Getters and setters
        public String getUserId() { return userId; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Set<SSOConfig.MFAConfig.MFAProvider> getEnabledProviders() { return enabledProviders; }
        public String getTotpSecret() { return totpSecret; }
        public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
        public boolean isTotpSetupPending() { return totpSetupPending; }
        public void setTotpSetupPending(boolean totpSetupPending) { this.totpSetupPending = totpSetupPending; }
        public Set<String> getBackupCodes() { return backupCodes; }
        public void setBackupCodes(Set<String> backupCodes) { this.backupCodes = backupCodes; }
    }

    private static class MFAChallenge {
        private final String challengeId;
        private final String userId;
        private final SSOConfig.MFAConfig.MFAProvider provider;
        private final Instant expiresAt;

        public MFAChallenge(String challengeId, String userId, SSOConfig.MFAConfig.MFAProvider provider, Instant expiresAt) {
            this.challengeId = challengeId;
            this.userId = userId;
            this.provider = provider;
            this.expiresAt = expiresAt;
        }

        // Getters
        public String getChallengeId() { return challengeId; }
        public String getUserId() { return userId; }
        public SSOConfig.MFAConfig.MFAProvider getProvider() { return provider; }
        public Instant getExpiresAt() { return expiresAt; }
    }

    public static class TOTPSetupResult {
        public final String secret;
        public final String qrCodeData;

        public TOTPSetupResult(String secret, String qrCodeData) {
            this.secret = secret;
            this.qrCodeData = qrCodeData;
        }
    }

    public static class MFAChallengeResult {
        private final boolean success;
        private final String error;
        private final String challengeId;
        private final String message;

        private MFAChallengeResult(boolean success, String error, String challengeId, String message) {
            this.success = success;
            this.error = error;
            this.challengeId = challengeId;
            this.message = message;
        }

        public static MFAChallengeResult success(String challengeId, String message) {
            return new MFAChallengeResult(true, null, challengeId, message);
        }

        public static MFAChallengeResult error(String error) {
            return new MFAChallengeResult(false, error, null, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
        public String getChallengeId() { return challengeId; }
        public String getMessage() { return message; }
    }

    public static class MFAVerificationResult {
        private final boolean success;
        private final String error;

        private MFAVerificationResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }

        public static MFAVerificationResult success() {
            return new MFAVerificationResult(true, null);
        }

        public static MFAVerificationResult failure(String error) {
            return new MFAVerificationResult(false, error);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
    }

    public static class MFAStatus {
        public final boolean enabled;
        public final Set<SSOConfig.MFAConfig.MFAProvider> enabledProviders;
        public final int backupCodesCount;
        public final boolean setupPending;

        public MFAStatus(boolean enabled, Set<SSOConfig.MFAConfig.MFAProvider> enabledProviders,
                        int backupCodesCount, boolean setupPending) {
            this.enabled = enabled;
            this.enabledProviders = enabledProviders;
            this.backupCodesCount = backupCodesCount;
            this.setupPending = setupPending;
        }
    }

    public static class MFAStatistics {
        public final boolean mfaEnabled;
        public final boolean mfaRequired;
        public final long usersWithMFA;
        public final int pendingChallenges;
        public final Set<SSOConfig.MFAConfig.MFAProvider> availableProviders;

        public MFAStatistics(boolean mfaEnabled, boolean mfaRequired, long usersWithMFA,
                           int pendingChallenges, Set<SSOConfig.MFAConfig.MFAProvider> availableProviders) {
            this.mfaEnabled = mfaEnabled;
            this.mfaRequired = mfaRequired;
            this.usersWithMFA = usersWithMFA;
            this.pendingChallenges = pendingChallenges;
            this.availableProviders = availableProviders;
        }
    }

    // Base32 encoding utility
    private static class Base32 {
        private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

        public static String encode(byte[] data) {
            StringBuilder result = new StringBuilder();
            int buffer = 0;
            int bufferLength = 0;

            for (byte b : data) {
                buffer = (buffer << 8) | (b & 0xFF);
                bufferLength += 8;

                while (bufferLength >= 5) {
                    result.append(ALPHABET.charAt((buffer >> (bufferLength - 5)) & 0x1F));
                    bufferLength -= 5;
                }
            }

            if (bufferLength > 0) {
                result.append(ALPHABET.charAt((buffer << (5 - bufferLength)) & 0x1F));
            }

            return result.toString();
        }

        public static byte[] decode(String encoded) {
            encoded = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");

            if (encoded.isEmpty()) {
                return new byte[0];
            }

            int outputLength = encoded.length() * 5 / 8;
            byte[] result = new byte[outputLength];

            int buffer = 0;
            int bufferLength = 0;
            int resultIndex = 0;

            for (char c : encoded.toCharArray()) {
                int value = ALPHABET.indexOf(c);
                if (value < 0) continue;

                buffer = (buffer << 5) | value;
                bufferLength += 5;

                if (bufferLength >= 8) {
                    result[resultIndex++] = (byte) ((buffer >> (bufferLength - 8)) & 0xFF);
                    bufferLength -= 8;
                }
            }

            return result;
        }
    }
}
