package dev.turtywurty.veldtlauncher.minecraft.launch.monitor.event;

import dev.turtywurty.veldtlauncher.event.Event;

public interface ProcessEvent extends Event {
    Process process();
}
