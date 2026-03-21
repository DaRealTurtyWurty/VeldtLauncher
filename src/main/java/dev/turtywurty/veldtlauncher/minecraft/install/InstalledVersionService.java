package dev.turtywurty.veldtlauncher.minecraft.install;

import java.nio.file.Path;

public interface InstalledVersionService {
    InstallResult isInstalled(String versionId, Path gameDirectory);
}