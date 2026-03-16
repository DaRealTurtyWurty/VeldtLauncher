package dev.turtywurty.minecraftlauncher.auth;

import dev.turtywurty.minecraftlauncher.event.EventStream;

public interface AuthStrategy {
    MinecraftSession authenticate() throws AuthException;
    boolean isAvailable();
    String displayName();
    EventStream eventStream();
}
