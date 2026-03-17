package dev.turtywurty.veldtlauncher.auth.devicecode.event;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;

public record DeviceCodeSucceededEvent(
        long expiresInSeconds,
        String tokenType,
        String scope
) implements AuthEvent {
}
