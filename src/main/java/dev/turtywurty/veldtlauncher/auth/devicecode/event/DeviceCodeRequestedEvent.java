package dev.turtywurty.veldtlauncher.auth.devicecode.event;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;

import java.net.URI;

public record DeviceCodeRequestedEvent(
        String userCode,
        URI verificationUri,
        URI verificationUriComplete,
        long expiresInSeconds,
        long pollingIntervalSeconds,
        String message
) implements AuthEvent {
}
