package dev.turtywurty.veldtlauncher.auth.pkce.event;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;

public record AuthenticationSucceededEvent(String username, String uuid) implements AuthEvent {
}
