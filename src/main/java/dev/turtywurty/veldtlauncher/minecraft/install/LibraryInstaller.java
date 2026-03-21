package dev.turtywurty.veldtlauncher.minecraft.install;

import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;

import java.nio.file.Path;

public interface LibraryInstaller {
    Path installLibraries(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException;
}
