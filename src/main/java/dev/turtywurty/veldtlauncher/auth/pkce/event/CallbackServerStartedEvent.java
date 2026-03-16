package dev.turtywurty.veldtlauncher.auth.pkce.event;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;

import java.net.URI;

public record CallbackServerStartedEvent(URI redirectUri) implements AuthEvent {
}
