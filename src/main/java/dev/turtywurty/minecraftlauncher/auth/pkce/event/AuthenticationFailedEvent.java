package dev.turtywurty.minecraftlauncher.auth.pkce.event;

import dev.turtywurty.minecraftlauncher.auth.AuthEvent;

public record AuthenticationFailedEvent(String message) implements AuthEvent {
}
