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
                isMissing(versionDirectory) ||
                        isMissing(versionJson) ||
                        isMissing(clientJar) ||
                        isMissing(librariesDirectory) ||
                        isMissing(assetsDirectory) ||
                        isMissing(nativesDirectory)
        );
    }

    public String getPartiallyInstalledReason() {
        if (isMissing(versionDirectory))
            return "Version directory is missing.";

        if (isMissing(versionJson))
            return "Version JSON is missing.";

        if (isMissing(clientJar))
            return "Client JAR is missing.";

        if (isMissing(librariesDirectory))
            return "Libraries directory is missing.";

        if (isMissing(assetsDirectory))
            return "Assets directory is missing.";

        if (isMissing(nativesDirectory))
            return "Natives directory is missing.";

        return "Unknown reason.";
    }

    private boolean isMissing(Path path) {
        return path == null || Files.notExists(path);
    }
}
