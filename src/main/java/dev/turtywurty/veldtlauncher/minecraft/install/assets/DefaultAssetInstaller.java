package dev.turtywurty.veldtlauncher.minecraft.install.assets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.turtywurty.veldtlauncher.instance.play.InstancePlayReporter;
import dev.turtywurty.veldtlauncher.instance.play.InstancePlayStep;
import dev.turtywurty.veldtlauncher.minecraft.install.VersionInstallException;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.AssetIndex;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.logging.Logging;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.logging.LoggingConfig;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.logging.LoggingFile;
import dev.turtywurty.veldtlauncher.util.DownloadUtil;
import dev.turtywurty.veldtlauncher.util.JsonUtil;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DefaultAssetInstaller implements AssetInstaller {
    private static final Gson GSON = new Gson();
    private final InstancePlayReporter reporter;

    public DefaultAssetInstaller() {
        this(InstancePlayReporter.noOp());
    }

    public DefaultAssetInstaller(InstancePlayReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void installAssets(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException {
        this.reporter.progress(InstancePlayStep.INSTALLING_ASSETS, "Preparing asset installation", 0D);
        AssetIndex assetIndex = metadata.assetIndex();
        if (assetIndex == null)
            throw new VersionInstallException("Asset index cannot be null");

        String assetIndexId = assetIndex.id();
        if (assetIndexId == null || assetIndexId.isBlank())
            throw new VersionInstallException("Asset index ID cannot be null or blank");

        Path assetsPath = gameDirectory.resolve(".minecraft").resolve("assets");

        Path indexesPath = assetsPath.resolve("indexes");
        Path assetIndexPath = indexesPath.resolve(assetIndexId + ".json");
        if (!Files.exists(assetIndexPath))
            throw new VersionInstallException("Asset index file does not exist for version " + metadata.id());

        AssetIndexes assetIndexes = parseAssetIndex(assetIndexPath);

        Path objectsPath = assetsPath.resolve("objects");
        Path tempObjectsPath = objectsPath.resolveSibling("objects.tmp");

        long sizeTally = 0;
        int totalObjects = assetIndexes.objects().size();
        int completedObjects = 0;
        for (AssetIndexes.IndexEntry entry : assetIndexes.objects()) {
            try {
                String hash = entry.hash();
                String key = entry.key();
                long size = entry.size();

                String twoCharHash = hash.substring(0, 2);
                Path objectPath = objectsPath.resolve(twoCharHash).resolve(hash);
                Path tempObjectPath = tempObjectsPath.resolve(twoCharHash).resolve(hash);

                if (Files.exists(objectPath)) {
                    this.reporter.log(InstancePlayStep.INSTALLING_ASSETS, "Checking asset " + key);
                    try {
                        if (!DownloadUtil.verifySHA1(objectPath, hash))
                            throw new VersionInstallException("Existing asset file SHA-1 hash does not match expected value for asset " + key + " in version " + metadata.id());

                        if (size > 0 && !DownloadUtil.verifyFileSize(objectPath, size))
                            throw new VersionInstallException("Existing asset file size does not match expected value for asset " + key + " in version " + metadata.id());

                        sizeTally += size;
                        completedObjects++;
                        this.reporter.progress(InstancePlayStep.INSTALLING_ASSETS, "Processed asset " + key, completedObjects, totalObjects);
                        continue;
                    } catch (Exception _) {
                        Files.deleteIfExists(objectPath);
                    }
                }

                this.reporter.log(InstancePlayStep.INSTALLING_ASSETS, "Downloading asset " + key);
                var url = new URI("https://resources.download.minecraft.net/" + twoCharHash + "/" + hash);
                DownloadUtil.downloadFile(url, tempObjectPath);
                if (!DownloadUtil.verifySHA1(tempObjectPath, hash))
                    throw new VersionInstallException("Downloaded asset file SHA-1 hash does not match expected value for asset " + key + " in version " + metadata.id());

                if (size > 0 && !DownloadUtil.verifyFileSize(tempObjectPath, size))
                    throw new VersionInstallException("Downloaded asset file size does not match expected value for asset " + key + " in version " + metadata.id());

                DownloadUtil.moveFile(tempObjectPath, objectPath);
                sizeTally += size;
                completedObjects++;
                this.reporter.progress(InstancePlayStep.INSTALLING_ASSETS, "Processed asset " + key, completedObjects, totalObjects);
            } catch (Exception exception) {
                throw new VersionInstallException("Failed to install asset " + entry.key() + " for version " + metadata.id(), exception);
            }
        }

        long expectedSize = assetIndex.totalSize();
        if (expectedSize > 0 && sizeTally != expectedSize)
            throw new VersionInstallException("Total size of installed assets does not match expected value for version " + metadata.id());

        installLoggingConfiguration(metadata, gameDirectory);
    }

    private AssetIndexes parseAssetIndex(Path assetIndexPath) throws VersionInstallException {
        try {
            String content = Files.readString(assetIndexPath);
            JsonObject json = GSON.fromJson(content, JsonObject.class);
            JsonObject objects = JsonUtil.getObject(json, "objects");
            List<AssetIndexes.IndexEntry> entries = new ArrayList<>(objects.size());
            for (String key : objects.keySet()) {
                JsonObject object = JsonUtil.getObject(objects, key);
                String hash = JsonUtil.getString(object, "hash");
                long size = JsonUtil.getLong(object, "size");
                entries.add(new AssetIndexes.IndexEntry(key, hash, size));
            }

            return new AssetIndexes(entries);
        } catch (Exception exception) {
            throw new VersionInstallException("Failed to parse asset index file for version " + assetIndexPath.getFileName(), exception);
        }
    }

    @Override
    public void installAssetIndex(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException {
        this.reporter.progress(InstancePlayStep.INSTALLING_ASSET_INDEX, "Checking asset index", 0D);
        AssetIndex assetIndex = metadata.assetIndex();
        if (assetIndex == null)
            throw new VersionInstallException("Asset index cannot be null");

        String assetIndexId = assetIndex.id();
        if (assetIndexId == null || assetIndexId.isBlank())
            throw new VersionInstallException("Asset index ID cannot be null or blank");

        URI url = assetIndex.url();
        if (url == null)
            throw new VersionInstallException("Asset index URL cannot be null");

        String sha1 = assetIndex.sha1();
        if (sha1 == null || sha1.isBlank())
            throw new VersionInstallException("Asset index SHA-1 cannot be null or blank");

        Path indexesPath = gameDirectory.resolve(".minecraft").resolve("assets").resolve("indexes");
        Path assetIndexPath = indexesPath.resolve(assetIndexId + ".json");
        Path tempAssetIndexPath = assetIndexPath.resolveSibling(assetIndexId + ".json.tmp");
        try {
            if (Files.exists(assetIndexPath)) {
                this.reporter.log(InstancePlayStep.INSTALLING_ASSET_INDEX, "Found existing asset index for " + metadata.id());
                try {
                    if (!DownloadUtil.verifySHA1(assetIndexPath, sha1))
                        throw new VersionInstallException("Existing asset index SHA-1 hash does not match expected value for version " + metadata.id());

                    long size = assetIndex.size();
                    if (size > 0 && !DownloadUtil.verifyFileSize(assetIndexPath, size))
                        throw new VersionInstallException("Existing asset index file size does not match expected value for version " + metadata.id());

                    this.reporter.progress(InstancePlayStep.INSTALLING_ASSET_INDEX, "Asset index is already up to date", 1D);
                    return;
                } catch (Exception _) {
                    Files.deleteIfExists(assetIndexPath);
                }
            }

            this.reporter.log(InstancePlayStep.INSTALLING_ASSET_INDEX, "Downloading asset index for " + metadata.id());
            DownloadUtil.downloadFile(url, tempAssetIndexPath);
            if (!DownloadUtil.verifySHA1(tempAssetIndexPath, sha1))
                throw new VersionInstallException("Downloaded asset index SHA-1 hash does not match expected value for version " + metadata.id());

            long size = assetIndex.size();
            if (size > 0 && !DownloadUtil.verifyFileSize(tempAssetIndexPath, size))
                throw new VersionInstallException("Downloaded asset index file size does not match expected value for version " + metadata.id());

            DownloadUtil.moveFile(tempAssetIndexPath, assetIndexPath);
            this.reporter.progress(InstancePlayStep.INSTALLING_ASSET_INDEX, "Asset index installed", 1D);
        } catch (Exception exception) {
            throw new VersionInstallException("Failed to install asset index for version " + metadata.id(), exception);
        }
    }

    private void installLoggingConfiguration(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException {
        this.reporter.progress(InstancePlayStep.INSTALLING_LOGGING, "Checking logging configuration", 0D);
        Logging logging = metadata.logging();
        if (logging == null)
            return;

        LoggingConfig clientLogging = logging.client();
        if (clientLogging == null)
            return;

        LoggingFile loggingFile = clientLogging.file();
        if (loggingFile == null || loggingFile.id() == null || loggingFile.id().isBlank())
            return;

        URI url = loggingFile.url();
        if (url == null)
            throw new VersionInstallException("Logging configuration URL is missing for version " + metadata.id());

        String sha1 = loggingFile.sha1();
        if (sha1 == null || sha1.isBlank())
            throw new VersionInstallException("Logging configuration SHA-1 is missing for version " + metadata.id());

        Path logConfigsDirectory = gameDirectory.resolve(".minecraft").resolve("assets").resolve("log_configs");
        Path loggingPath = logConfigsDirectory.resolve(loggingFile.id());
        Path tempLoggingPath = loggingPath.resolveSibling(loggingFile.id() + ".tmp");
        try {
            Files.createDirectories(logConfigsDirectory);

            if (Files.exists(loggingPath)) {
                this.reporter.log(InstancePlayStep.INSTALLING_LOGGING, "Found existing logging configuration for " + metadata.id());
                try {
                    if (!DownloadUtil.verifySHA1(loggingPath, sha1))
                        throw new VersionInstallException("Existing logging configuration SHA-1 hash does not match expected value for version " + metadata.id());

                    long size = loggingFile.size();
                    if (size > 0 && !DownloadUtil.verifyFileSize(loggingPath, size))
                        throw new VersionInstallException("Existing logging configuration file size does not match expected value for version " + metadata.id());

                    this.reporter.progress(InstancePlayStep.INSTALLING_LOGGING, "Logging configuration is already up to date", 1D);
                    return;
                } catch (Exception _) {
                    Files.deleteIfExists(loggingPath);
                }
            }

            this.reporter.log(InstancePlayStep.INSTALLING_LOGGING, "Downloading logging configuration for " + metadata.id());
            DownloadUtil.downloadFile(url, tempLoggingPath);
            if (!DownloadUtil.verifySHA1(tempLoggingPath, sha1))
                throw new VersionInstallException("Downloaded logging configuration SHA-1 hash does not match expected value for version " + metadata.id());

            long size = loggingFile.size();
            if (size > 0 && !DownloadUtil.verifyFileSize(tempLoggingPath, size))
                throw new VersionInstallException("Downloaded logging configuration file size does not match expected value for version " + metadata.id());

            DownloadUtil.moveFile(tempLoggingPath, loggingPath);
            this.reporter.progress(InstancePlayStep.INSTALLING_LOGGING, "Logging configuration installed", 1D);
        } catch (VersionInstallException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new VersionInstallException("Failed to install logging configuration for version " + metadata.id(), exception);
        } finally {
            try {
                Files.deleteIfExists(tempLoggingPath);
            } catch (IOException ignored) {
            }
        }
    }
}
