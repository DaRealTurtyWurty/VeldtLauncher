package dev.turtywurty.minecraftlauncher.auth.pkce.event;

import dev.turtywurty.minecraftlauncher.auth.AuthEvent;

import java.net.URI;

public record CallbackServerStartedEvent(URI redirectUri) implements AuthEvent {
}
