package dev.turtywurty.veldtlauncher.minecraft.manifest;

import java.net.URI;
import java.time.Instant;

public record VersionManifestEntry(
        String id,
        String type,
        URI url,
        String sha1,
        Instant releaseTime,
        Instant time,
        int complianceLevel
) {
    public VersionType getVersionType() {
        return switch (type) {
            case "release" -> VersionType.RELEASE;
            case "snapshot" -> VersionType.SNAPSHOT;
            case "old_beta" -> VersionType.OLD_BETA;
            case "old_alpha" -> VersionType.OLD_ALPHA;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    public enum VersionType {
        RELEASE,
        SNAPSHOT,
        OLD_BETA,
        OLD_ALPHA
    }
}
