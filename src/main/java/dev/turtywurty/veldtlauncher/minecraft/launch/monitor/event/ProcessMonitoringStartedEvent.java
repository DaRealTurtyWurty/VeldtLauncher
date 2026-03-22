package dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event;

public record ProcessMonitoringStartedEvent(Process process) implements ProcessEvent {
}
