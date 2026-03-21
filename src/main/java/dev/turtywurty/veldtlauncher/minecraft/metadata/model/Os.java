package dev.turtywurty.veldtlauncher.minecraft.metadata.model;

public record Os(
        String name,
        String arch,
        VersionRange versionRange
) {
}
