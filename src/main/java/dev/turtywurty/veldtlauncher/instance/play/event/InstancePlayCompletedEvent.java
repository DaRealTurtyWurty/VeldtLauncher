package dev.turtywurty.veldtlauncher.instance.play.event;

import dev.turtywurty.veldtlauncher.event.Event;
import dev.turtywurty.veldtlauncher.minecraft.launch.monitor.ProcessMonitorHandle;
import dev.turtywurty.veldtlauncher.minecraft.mapping.Mappings;

public record InstancePlayCompletedEvent(
        String message,
        ProcessMonitorHandle monitorHandle,
        String logsWindowTitle,
        Mappings mappings
) implements Event {
}
