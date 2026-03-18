package dev.turtywurty.veldtlauncher.auth.event;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;

public record MinecraftProfileFetchedEvent(String username, String uuid) implements AuthEvent {
}
