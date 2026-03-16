package dev.turtywurty.veldtlauncher.auth;

import dev.turtywurty.veldtlauncher.event.EventStream;

public interface AuthStrategy {
    MinecraftSession authenticate() throws AuthException;
    boolean isAvailable();
    String displayName();
    EventStream eventStream();
}
