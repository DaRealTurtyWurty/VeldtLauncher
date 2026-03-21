package dev.turtywurty.veldtlauncher.minecraft.install.assets;

import dev.turtywurty.veldtlauncher.minecraft.install.VersionInstallException;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;

import java.nio.file.Path;

public interface AssetInstaller {
    void installAssets(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException;

    void installAssetIndex(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException;
}
