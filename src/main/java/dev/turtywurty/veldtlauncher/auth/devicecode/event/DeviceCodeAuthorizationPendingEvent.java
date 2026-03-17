package dev.turtywurty.veldtlauncher.auth.devicecode.event;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;

public record DeviceCodeAuthorizationPendingEvent(
        long pollingIntervalSeconds
) implements AuthEvent {
}
