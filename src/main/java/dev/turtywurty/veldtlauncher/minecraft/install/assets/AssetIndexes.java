package dev.turtywurty.veldtlauncher.minecraft.install.assets;

import java.util.List;

public record AssetIndexes(
        List<IndexEntry> objects
) {
    public record IndexEntry(
            String key,
            String hash,
            long size
    ) {
    }
}
