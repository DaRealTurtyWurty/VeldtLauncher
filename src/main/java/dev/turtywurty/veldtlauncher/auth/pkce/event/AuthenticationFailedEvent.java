package dev.turtywurty.veldtlauncher.auth.pkce.event;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;

public record AuthenticationFailedEvent(String message) implements AuthEvent {
}
