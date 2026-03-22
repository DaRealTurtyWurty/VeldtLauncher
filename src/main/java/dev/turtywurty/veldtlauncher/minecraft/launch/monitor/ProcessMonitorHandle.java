package dev.turtywurty.veldtlauncher.minecraft.launch.monitor;

import dev.turtywurty.veldtlauncher.event.Event;
import dev.turtywurty.veldtlauncher.event.EventStream;

public interface ProcessMonitorHandle {
    boolean isAlive();

    void stopMonitoring();

    EventStream eventStream();

    void emit(Event event);

    void addMonitoringThread(Thread thread);
}