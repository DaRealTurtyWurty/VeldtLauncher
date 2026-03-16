package dev.turtywurty.veldtlauncher.auth.pkce.event;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;

public record AuthorizationCallbackReceivedEvent(boolean error) implements AuthEvent {
}
