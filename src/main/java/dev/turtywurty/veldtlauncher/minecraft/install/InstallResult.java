package dev.turtywurty.veldtlauncher.minecraft.install;

import java.nio.file.Files;
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
    public boolean isPartiallyInstalled() {
        return !newlyInstalled && (
                Files.notExists(versionJson) ||
                        Files.notExists(clientJar) ||
                        Files.notExists(librariesDirectory) ||
                        Files.notExists(assetsDirectory) ||
                        Files.notExists(nativesDirectory)
        );
    }

    public String getPartiallyInstalledReason() {
        if (Files.notExists(versionJson))
            return "Version JSON is missing.";

        if (Files.notExists(clientJar))
            return "Client JAR is missing.";

        if (Files.notExists(librariesDirectory))
            return "Libraries directory is missing.";

        if (Files.notExists(assetsDirectory))
            return "Assets directory is missing.";

        if (Files.notExists(nativesDirectory))
            return "Natives directory is missing.";

        return "Unknown reason.";
    }
}