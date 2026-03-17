package dev.turtywurty.veldtlauncher.auth.devicecode.event;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;

public record DeviceCodePollingStartedEvent(
        String userCode,
        long pollingIntervalSeconds,
        long expiresInSeconds
) implements AuthEvent {
}
