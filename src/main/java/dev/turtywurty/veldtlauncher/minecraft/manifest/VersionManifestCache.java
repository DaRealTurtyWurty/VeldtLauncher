package dev.turtywurty.veldtlauncher.minecraft.manifest;

import java.util.Optional;

public interface VersionManifestCache {
    Optional<VersionManifest> getManifest();

    void putManifest(VersionManifest manifest);

    void clear();
}
