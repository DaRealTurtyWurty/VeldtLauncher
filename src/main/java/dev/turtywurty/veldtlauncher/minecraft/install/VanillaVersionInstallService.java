package dev.turtywurty.veldtlauncher.minecraft.install;

import dev.turtywurty.veldtlauncher.instance.play.InstancePlayReporter;
import dev.turtywurty.veldtlauncher.instance.play.InstancePlayStep;
import dev.turtywurty.veldtlauncher.minecraft.install.assets.AssetInstaller;
import dev.turtywurty.veldtlauncher.minecraft.install.assets.DefaultAssetInstaller;
import dev.turtywurty.veldtlauncher.minecraft.manifest.VersionManifestService;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;

import java.nio.file.Files;
import java.nio.file.Path;

public class VanillaVersionInstallService implements VersionInstallService {
    private final VersionManifestService versionManifestService;
    private final ClientInstaller clientInstaller;
    private final LibraryInstaller libraryInstaller;
    private final AssetInstaller assetInstaller;
    private final InstancePlayReporter reporter;

    public VanillaVersionInstallService(
            VersionManifestService versionManifestService,
            ClientInstaller clientInstaller,
            LibraryInstaller libraryInstaller,
            AssetInstaller assetInstaller,
            InstancePlayReporter reporter
    ) {
        this.versionManifestService = versionManifestService;
        this.clientInstaller = clientInstaller;
        this.libraryInstaller = libraryInstaller;
        this.assetInstaller = assetInstaller;
        this.reporter = reporter;
    }

    public VanillaVersionInstallService(VersionManifestService versionManifestService) {
        this(versionManifestService, InstancePlayReporter.noOp());
    }

    public VanillaVersionInstallService(VersionManifestService versionManifestService, InstancePlayReporter reporter) {
        this(
                versionManifestService,
                new DefaultClientInstaller(reporter),
                new DefaultLibraryInstaller(reporter),
                new DefaultAssetInstaller(reporter),
                reporter
        );
    }

    @Override
    public InstallResult install(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException {
        validateManifest(metadata);
        validateGameDirectory(gameDirectory);
        this.reporter.progress(InstancePlayStep.PREPARING_DIRECTORIES, "Preparing instance directories", 0D);
        createDirectories(gameDirectory, metadata);
        this.reporter.progress(InstancePlayStep.PREPARING_DIRECTORIES, "Instance directories are ready", 1D);
        this.reporter.progress(InstancePlayStep.SAVING_METADATA, "Saving version metadata", 0D);
        writeVersionMetadata(metadata);
        this.reporter.progress(InstancePlayStep.SAVING_METADATA, "Version metadata saved", 1D);
        installClient(metadata, gameDirectory);
        installLibraries(metadata, gameDirectory);
        installAssetIndex(metadata, gameDirectory);
        installAssets(metadata, gameDirectory);

        Path minecraftDirectory = resolveMinecraftDirectory(gameDirectory);
        Path versionDirectory = minecraftDirectory.resolve("versions").resolve(metadata.id());
        return new InstallResult(
                metadata.id(),
                gameDirectory,
                versionDirectory,
                versionDirectory.resolve(metadata.id() + ".json"),
                versionDirectory.resolve(metadata.id() + ".jar"),
                minecraftDirectory.resolve("libraries"),
                minecraftDirectory.resolve("assets"),
                versionDirectory.resolve("natives"),
                true
        );
    }

    private void validateManifest(VersionMetadata metadata) throws VersionInstallException {
        if (metadata == null)
            throw new VersionInstallException("Metadata cannot be null");

        boolean exists = versionManifestService.fetchVersion(metadata.id()).isPresent();
        if (!exists)
            throw new VersionInstallException("Version " + metadata.id() + " does not exist in the manifest");
    }

    private void validateGameDirectory(Path gameDirectory) throws VersionInstallException {
        if (gameDirectory == null)
            throw new VersionInstallException("Game directory cannot be null");

        if (Files.exists(gameDirectory) && !Files.isDirectory(gameDirectory))
            throw new VersionInstallException("Game directory is not a directory");

        if (Files.exists(gameDirectory) && !Files.isReadable(gameDirectory))
            throw new VersionInstallException("Game directory is not readable");

        if (Files.exists(gameDirectory) && !Files.isWritable(gameDirectory))
            throw new VersionInstallException("Game directory is not writable");
    }

    private void createDirectories(Path gameDirectory, VersionMetadata metadata) throws VersionInstallException {
        Path minecraftDirectory = resolveMinecraftDirectory(gameDirectory);
        Path versionsDirectory = minecraftDirectory.resolve("versions");
        Path versionDirectory = versionsDirectory.resolve(metadata.id());
        Path librariesDirectory = minecraftDirectory.resolve("libraries");
        Path assetsDirectory = minecraftDirectory.resolve("assets");
        Path indexesDirectory = assetsDirectory.resolve("indexes");
        Path objectsDirectory = assetsDirectory.resolve("objects");
        Path nativesDirectory = versionDirectory.resolve("natives");

        try {
            Files.createDirectories(versionDirectory);
            Files.createDirectories(librariesDirectory);
            Files.createDirectories(indexesDirectory);
            Files.createDirectories(objectsDirectory);
            Files.createDirectories(nativesDirectory);
        } catch (Exception exception) {
            throw new VersionInstallException("Failed to create necessary directories", exception);
        }
    }

    private Path resolveMinecraftDirectory(Path gameDirectory) {
        return gameDirectory.resolve(".minecraft");
    }

    private void writeVersionMetadata(VersionMetadata metadata) throws VersionInstallException {
        try {
            versionManifestService.saveVersionMetadata(metadata);
        } catch (Exception exception) {
            throw new VersionInstallException("Failed to save version metadata", exception);
        }
    }

    private void installClient(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException {
        try {
            clientInstaller.installClient(metadata, gameDirectory);
        } catch (Exception exception) {
            throw new VersionInstallException("Failed to install client", exception);
        }
    }

    private void installLibraries(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException {
        try {
            libraryInstaller.installLibraries(metadata, gameDirectory);
        } catch (Exception exception) {
            throw new VersionInstallException("Failed to install libraries", exception);
        }
    }

    private void installAssetIndex(VersionMetadata metadata, Path gameDirectory) {
        try {
            assetInstaller.installAssetIndex(metadata, gameDirectory);
        } catch (Exception exception) {
            throw new VersionInstallException("Failed to install asset index", exception);
        }
    }

    private void installAssets(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException {
        try {
            assetInstaller.installAssets(metadata, gameDirectory);
        } catch (Exception exception) {
            throw new VersionInstallException("Failed to install assets", exception);
        }
    }
}
