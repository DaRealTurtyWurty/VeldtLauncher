package dev.turtywurty.minecraftlauncher.auth.pkce.event;

import dev.turtywurty.minecraftlauncher.auth.AuthEvent;

public record AuthenticationSucceededEvent(String username, String uuid) implements AuthEvent {
}
