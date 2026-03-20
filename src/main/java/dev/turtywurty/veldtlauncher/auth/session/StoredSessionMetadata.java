package dev.turtywurty.veldtlauncher.auth.session;

public record StoredSessionMetadata(
        String userId,
        String username,
        long expiresAt,
        String accountId,
        long lastAccessedAt,
        String skinUrl
) {
}
