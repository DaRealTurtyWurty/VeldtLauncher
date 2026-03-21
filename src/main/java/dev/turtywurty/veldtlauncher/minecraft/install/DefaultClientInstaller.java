package dev.turtywurty.veldtlauncher.minecraft.install;

import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.download.Download;
import dev.turtywurty.veldtlauncher.util.DownloadUtil;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultClientInstaller implements ClientInstaller {
    @Override
    public Path installClient(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException {
        Download client = metadata.downloads().client();
        if (client == null)
            throw new VersionInstallException("Client download information is missing for version " + metadata.id());

        String sha1 = client.sha1();
        if (sha1 == null || sha1.isEmpty())
            throw new VersionInstallException("Client download SHA-1 hash is missing for version " + metadata.id());

        URI url = client.url();
        if (url == null)
            throw new VersionInstallException("Client download URL is missing for version " + metadata.id());

        Path versionDirectory = gameDirectory.resolve(".minecraft").resolve("versions").resolve(metadata.id());
        Path clientJarPath = versionDirectory.resolve(metadata.id() + ".jar");
        Path tempFilePath = versionDirectory.resolve(metadata.id() + ".jar.tmp");
        try {
            if (Files.exists(clientJarPath)) {
                try {
                    if (!DownloadUtil.verifySHA1(clientJarPath, sha1))
                        throw new VersionInstallException("Existing client JAR SHA-1 hash does not match expected value for version " + metadata.id());

                    long size = client.size();
                    if (size > 0 && !DownloadUtil.verifyFileSize(clientJarPath, size))
                        throw new VersionInstallException("Existing client JAR file size does not match expected value for version " + metadata.id());

                    return clientJarPath;
                } catch (Exception _) {
                    Files.deleteIfExists(clientJarPath);
                }
            }

            DownloadUtil.downloadFile(url, tempFilePath);
            if (!DownloadUtil.verifySHA1(tempFilePath, sha1))
                throw new VersionInstallException("Client download SHA-1 hash does not match expected value for version " + metadata.id());

            long size = client.size();
            if (size > 0 && !DownloadUtil.verifyFileSize(tempFilePath, size))
                throw new VersionInstallException("Client download file size does not match expected value for version " + metadata.id());

            DownloadUtil.moveFile(tempFilePath, clientJarPath);
            return clientJarPath;
        } catch (Exception exception) {
            throw new VersionInstallException("Failed to install client for version " + metadata.id(), exception);
        }
    }
}
