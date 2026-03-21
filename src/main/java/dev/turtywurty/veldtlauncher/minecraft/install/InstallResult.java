package dev.turtywurty.veldtlauncher.minecraft.install;

import java.nio.file.Path;

public record InstallResult(
        String versionId,
        Path gameDirectory,
        Path versionDirectory,
        Path versionJson,
        Path clientJar,
        Path librariesDirectory,
        Path assetsDirectory,
        Path nativesDirectory,
        boolean newlyInstalled
) {
}