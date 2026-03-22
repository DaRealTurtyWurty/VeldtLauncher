package dev.turtywurty.veldtlauncher.minecraft.launch.monitor;

public interface ProcessMonitor {
    ProcessMonitorHandle attach(Process process);
}
