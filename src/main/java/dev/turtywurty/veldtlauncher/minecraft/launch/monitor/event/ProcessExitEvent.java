package dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event;

public record ProcessExitEvent(
        Process process,
        int exitCode
) implements ProcessEvent {
}
