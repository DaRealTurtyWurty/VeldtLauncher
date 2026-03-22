package dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event;

import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.ProcessOutputLine;

public record ProcessOutputEvent(
        Process process,
        ProcessOutputLine line
) implements ProcessEvent {
}
