package dev.turtywurty.minecraftlauncher.auth.pkce.event;

import dev.turtywurty.minecraftlauncher.auth.AuthEvent;

public record AuthorizationCallbackReceivedEvent(boolean error) implements AuthEvent {
}
