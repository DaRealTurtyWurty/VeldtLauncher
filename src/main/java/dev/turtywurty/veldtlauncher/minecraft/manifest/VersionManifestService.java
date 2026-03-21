package dev.turtywurty.veldtlauncher.minecraft.manifest;

import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;

import java.util.List;
import java.util.Optional;

public interface VersionManifestService {
    VersionManifest fetchManifest() throws VersionManifestException;

    List<VersionManifestEntry> fetchVersions() throws VersionManifestException;

    Optional<VersionManifestEntry> fetchVersion(String versionId) throws VersionManifestException;

    Optional<VersionManifestEntry> fetchLatestRelease() throws VersionManifestException;

    Optional<VersionManifestEntry> fetchLatestSnapshot() throws VersionManifestException;

    VersionMetadata fetchVersionMetadata(String versionId) throws VersionManifestException;

    VersionMetadata fetchVersionMetadata(VersionManifestEntry entry) throws VersionManifestException;

    void saveVersionMetadata(VersionMetadata metadata);
}
