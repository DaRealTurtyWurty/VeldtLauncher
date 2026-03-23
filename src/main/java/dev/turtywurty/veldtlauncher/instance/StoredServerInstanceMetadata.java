package dev.turtywurty.veldtlauncher.instance;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public record StoredServerInstanceMetadata(
        String id,
        String name,
        String address,
        String minecraftVersion,
        List<String> supportedInstanceIds,
        Path gameDirectory,
        boolean managed,
        String iconPath,
        Instant createdAt,
        Instant lastPlayedAt
) implements StoredInstanceMetadata {
    @Override
    public InstanceType type() {
        return InstanceType.SERVER;
    }

    @Override
    public StoredInstanceMetadata withLastPlayedAt(Instant lastPlayedAt) {
        return new StoredServerInstanceMetadata(
                this.id,
                this.name,
                this.address,
                this.minecraftVersion,
                this.supportedInstanceIds,
                this.gameDirectory,
                this.managed,
                this.iconPath,
                this.createdAt,
                lastPlayedAt
        );
    }
}
