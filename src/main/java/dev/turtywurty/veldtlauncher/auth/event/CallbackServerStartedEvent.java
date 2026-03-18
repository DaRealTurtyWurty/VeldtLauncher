package dev.turtywurty.veldtlauncher.auth.event;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;

import java.net.URI;

public record CallbackServerStartedEvent(URI redirectUri) implements AuthEvent {
}
