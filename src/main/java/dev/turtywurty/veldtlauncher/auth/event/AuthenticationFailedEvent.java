package dev.turtywurty.veldtlauncher.auth.event;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;

public record AuthenticationFailedEvent(String message) implements AuthEvent {
}
