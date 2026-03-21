package dev.turtywurty.veldtlauncher.minecraft.manifest;

import java.util.List;

public record VersionManifest(
        LatestVersions latest,
        List<VersionManifestEntry> versions
) {
}
