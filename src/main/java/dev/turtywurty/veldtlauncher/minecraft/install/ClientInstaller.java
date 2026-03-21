package dev.turtywurty.veldtlauncher.minecraft.install;

import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;

import java.nio.file.Path;

public interface ClientInstaller {
    Path installClient(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException;
}
