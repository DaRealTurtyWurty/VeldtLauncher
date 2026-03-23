package dev.turtywurty.veldtlauncher.instance;

import java.nio.file.Path;
import java.time.Instant;

public record StoredModpackInstanceMetadata(
        String id,
        String name,
        String minecraftVersion,
        ModLoader modLoader,
        Path gameDirectory,
        boolean managed,
        String iconPath,
        Instant createdAt,
        Instant lastPlayedAt
) implements StoredInstanceMetadata {
    @Override
    public InstanceType type() {
        return InstanceType.MODPACK;
    }

    @Override
    public StoredInstanceMetadata withLastPlayedAt(Instant lastPlayedAt) {
        return new StoredModpackInstanceMetadata(
                this.id,
                this.name,
                this.minecraftVersion,
                this.modLoader,
                this.gameDirectory,
                this.managed,
                this.iconPath,
                this.createdAt,
                lastPlayedAt
        );
    }
}
