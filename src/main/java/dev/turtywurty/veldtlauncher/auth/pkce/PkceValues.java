package dev.turtywurty.veldtlauncher.auth.pkce;

public record PkceValues(
        String codeVerifier,
        String codeChallenge,
        String codeChallengeMethod
) {
    public PkceValues(String codeVerifier, String codeChallenge) {
        this(codeVerifier, codeChallenge, "S256");
    }
}
