package dev.turtywurty.veldtlauncher.auth.pkce.event;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;

public record MinecraftProfileFetchedEvent(String username, String uuid) implements AuthEvent {
}
