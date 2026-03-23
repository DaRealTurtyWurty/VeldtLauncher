package dev.turtywurty.veldtlauncher.minecraft.install;

import dev.turtywurty.veldtlauncher.instance.play.InstancePlayReporter;
import dev.turtywurty.veldtlauncher.instance.play.InstancePlayStep;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.download.Download;
import dev.turtywurty.veldtlauncher.util.DownloadUtil;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultClientInstaller implements ClientInstaller {
    private final InstancePlayReporter reporter;

    public DefaultClientInstaller() {
        this(InstancePlayReporter.noOp());
    }

    public DefaultClientInstaller(InstancePlayReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public Path installClient(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException {
        this.reporter.progress(InstancePlayStep.INSTALLING_CLIENT, "Checking client jar", 0D);
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
                this.reporter.log(InstancePlayStep.INSTALLING_CLIENT, "Found existing client jar for " + metadata.id());
                try {
                    if (!DownloadUtil.verifySHA1(clientJarPath, sha1))
                        throw new VersionInstallException("Existing client JAR SHA-1 hash does not match expected value for version " + metadata.id());

                    long size = client.size();
                    if (size > 0 && !DownloadUtil.verifyFileSize(clientJarPath, size))
                        throw new VersionInstallException("Existing client JAR file size does not match expected value for version " + metadata.id());

                    installClientMappings(metadata, versionDirectory);
                    this.reporter.progress(InstancePlayStep.INSTALLING_CLIENT, "Client jar is already up to date", 1D);
                    return clientJarPath;
                } catch (Exception _) {
                    Files.deleteIfExists(clientJarPath);
                }
            }

            this.reporter.log(InstancePlayStep.INSTALLING_CLIENT, "Downloading client jar for " + metadata.id());
            DownloadUtil.downloadFile(url, tempFilePath);
            if (!DownloadUtil.verifySHA1(tempFilePath, sha1))
                throw new VersionInstallException("Client download SHA-1 hash does not match expected value for version " + metadata.id());

            long size = client.size();
            if (size > 0 && !DownloadUtil.verifyFileSize(tempFilePath, size))
                throw new VersionInstallException("Client download file size does not match expected value for version " + metadata.id());

            DownloadUtil.moveFile(tempFilePath, clientJarPath);
            installClientMappings(metadata, versionDirectory);
            this.reporter.progress(InstancePlayStep.INSTALLING_CLIENT, "Client jar installed", 1D);
            return clientJarPath;
        } catch (Exception exception) {
            throw new VersionInstallException("Failed to install client for version " + metadata.id(), exception);
        }
    }

    private void installClientMappings(VersionMetadata metadata, Path versionDirectory) {
        if (metadata == null || metadata.downloads() == null)
            return;

        Download clientMappings = metadata.downloads().clientMappings();
        if (clientMappings == null || clientMappings.url() == null)
            return;

        Path mappingsPath = versionDirectory.resolve(metadata.id() + ".mappings.txt");
        Path tempMappingsPath = versionDirectory.resolve(metadata.id() + ".mappings.txt.tmp");
        try {
            if (Files.exists(mappingsPath)) {
                this.reporter.log(InstancePlayStep.INSTALLING_CLIENT, "Found existing client mappings for " + metadata.id());
                try {
                    if (!isDownloadValid(mappingsPath, clientMappings))
                        throw new VersionInstallException("Existing client mappings do not match expected metadata for version " + metadata.id());

                    return;
                } catch (Exception _) {
                    Files.deleteIfExists(mappingsPath);
                }
            }

            this.reporter.log(InstancePlayStep.INSTALLING_CLIENT, "Downloading client mappings for " + metadata.id());
            DownloadUtil.downloadFile(clientMappings.url(), tempMappingsPath);
            validateDownloadedFile(tempMappingsPath, clientMappings, "client mappings", metadata.id());
            DownloadUtil.moveFile(tempMappingsPath, mappingsPath);
        } catch (Exception exception) {
            this.reporter.error(
                    InstancePlayStep.INSTALLING_CLIENT,
                    "Failed to download client mappings for " + metadata.id() + ": " + exception.getMessage()
            );
        } finally {
            try {
                Files.deleteIfExists(tempMappingsPath);
            } catch (IOException ignored) {
            }
        }
    }

    private boolean isDownloadValid(Path filePath, Download download) throws IOException {
        String sha1 = download.sha1();
        if (sha1 != null && !sha1.isBlank() && !DownloadUtil.verifySHA1(filePath, sha1))
            return false;

        long size = download.size();
        return size <= 0 || DownloadUtil.verifyFileSize(filePath, size);
    }

    private void validateDownloadedFile(Path filePath, Download download, String description, String versionId)
            throws VersionInstallException, IOException {
        String sha1 = download.sha1();
        if (sha1 != null && !sha1.isBlank() && !DownloadUtil.verifySHA1(filePath, sha1))
            throw new VersionInstallException("Downloaded " + description + " SHA-1 hash does not match expected value for version " + versionId);

        long size = download.size();
        if (size > 0 && !DownloadUtil.verifyFileSize(filePath, size))
            throw new VersionInstallException("Downloaded " + description + " file size does not match expected value for version " + versionId);
    }
}
