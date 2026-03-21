package dev.turtywurty.veldtlauncher.minecraft.install;

import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultInstalledVersionService implements InstalledVersionService {
    @Override
    public InstallResult isInstalled(String versionId, Path gameDirectory) {
        if (versionId == null || versionId.isBlank() || gameDirectory == null)
            return null;

        Path minecraftDirectory = gameDirectory.resolve(".minecraft");
        Path versionDirectory = minecraftDirectory.resolve("versions").resolve(versionId);
        Path versionJson = versionDirectory.resolve(versionId + ".json");
        Path clientJar = versionDirectory.resolve(versionId + ".jar");
        Path librariesDirectory = minecraftDirectory.resolve("libraries");
        Path assetsDirectory = minecraftDirectory.resolve("assets");
        Path nativesDirectory = versionDirectory.resolve("natives");

        return new InstallResult(
                versionId,
                gameDirectory,
                Files.isDirectory(versionDirectory) ? versionDirectory : null,
                Files.isRegularFile(versionJson) ? versionJson : null,
                Files.isRegularFile(clientJar) ? clientJar : null,
                Files.isDirectory(librariesDirectory) ? librariesDirectory : null,
                Files.isDirectory(assetsDirectory) ? assetsDirectory : null,
                Files.isDirectory(nativesDirectory) ? nativesDirectory : null,
                false
        );
    }
}
