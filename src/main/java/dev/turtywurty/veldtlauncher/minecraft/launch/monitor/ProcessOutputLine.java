package dev.turtywurty.veldtlauncher.minecraft.launch.monitor;

public record ProcessOutputLine(
        StreamType streamType,
        String line
) {
    public enum StreamType {
        STDOUT,
        STDERR
    }
}