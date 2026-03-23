package dev.turtywurty.veldtlauncher.instance;

import java.nio.file.Path;
import java.time.Instant;

public sealed interface StoredInstanceMetadata
        permits StoredVanillaInstanceMetadata, StoredModpackInstanceMetadata, StoredServerInstanceMetadata {
    String id();

    String name();

    InstanceType type();

    Path gameDirectory();

    boolean managed();

    String iconPath();

    Instant createdAt();

    Instant lastPlayedAt();

    StoredInstanceMetadata withLastPlayedAt(Instant lastPlayedAt);
}
