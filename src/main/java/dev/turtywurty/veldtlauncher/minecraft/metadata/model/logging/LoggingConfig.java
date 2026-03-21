package dev.turtywurty.veldtlauncher.minecraft.metadata.model.logging;

public record LoggingConfig(
        String argument,
        LoggingFile file,
        String type
) {
}
