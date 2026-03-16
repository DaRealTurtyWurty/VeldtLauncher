package dev.turtywurty.minecraftlauncher.auth.pkce.event;

import dev.turtywurty.minecraftlauncher.auth.AuthEvent;

public record MinecraftProfileFetchedEvent(String username, String uuid) implements AuthEvent {
}
