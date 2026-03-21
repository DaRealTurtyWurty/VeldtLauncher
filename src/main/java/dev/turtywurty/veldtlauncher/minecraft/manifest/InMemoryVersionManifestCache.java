package dev.turtywurty.veldtlauncher.minecraft.manifest;

import java.util.Optional;

public class InMemoryVersionManifestCache implements VersionManifestCache {
    private VersionManifest manifest;

    @Override
    public Optional<VersionManifest> getManifest() {
        return Optional.ofNullable(manifest);
    }

    @Override
    public void putManifest(VersionManifest manifest) {
        this.manifest = manifest;
    }

    @Override
    public void clear() {
        this.manifest = null;
    }
}
