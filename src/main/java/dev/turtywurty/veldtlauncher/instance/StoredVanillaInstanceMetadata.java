package dev.turtywurty.veldtlauncher.instance;

import java.nio.file.Path;
import java.time.Instant;

public record StoredVanillaInstanceMetadata(
        String id,
        String name,
        String minecraftVersion,
        Path gameDirectory,
        boolean managed,
        String iconPath,
        Instant createdAt,
        Instant lastPlayedAt
) implements StoredInstanceMetadata {
    @Override
    public InstanceType type() {
        return InstanceType.VANILLA;
    }

    @Override
    public StoredInstanceMetadata withLastPlayedAt(Instant lastPlayedAt) {
        return new StoredVanillaInstanceMetadata(
                this.id,
                this.name,
                this.minecraftVersion,
                this.gameDirectory,
                this.managed,
                this.iconPath,
                this.createdAt,
                lastPlayedAt
        );
    }
}
