package dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event;

public record ProcessMonitoringFailedEvent(
        Process process,
        Throwable cause
) implements ProcessEvent {
}
