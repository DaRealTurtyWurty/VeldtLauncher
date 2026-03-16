package dev.turtywurty.minecraftlauncher.auth.pkce;

import java.net.URI;
import java.time.Instant;

public record PendingLogin(
        PkceValues pkce,
        String state,
        URI redirectUri,
        Instant startedAt
) {
    public PendingLogin(PkceValues pkce, String state, URI redirectUri) {
        this(pkce, state, redirectUri, Instant.now());
    }
}
