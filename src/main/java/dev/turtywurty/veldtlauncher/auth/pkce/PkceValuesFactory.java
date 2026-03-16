package dev.turtywurty.veldtlauncher.auth.pkce;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PkceValuesFactory implements PkceValuesProvider {
    private final SecureRandom secureRandom;

    public PkceValuesFactory() {
        this(new SecureRandom());
    }

    public PkceValuesFactory(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public PkceValues create() {
        String verifier = generateRandomVerifier();
        String challenge = generateCodeChallenge(verifier);
        return new PkceValues(verifier, challenge);
    }

    private String generateRandomVerifier() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}
