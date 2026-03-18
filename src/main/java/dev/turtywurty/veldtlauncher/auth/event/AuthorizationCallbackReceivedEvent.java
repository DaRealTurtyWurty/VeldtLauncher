package dev.turtywurty.veldtlauncher.auth.event;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;

public record AuthorizationCallbackReceivedEvent(boolean error) implements AuthEvent {
}
