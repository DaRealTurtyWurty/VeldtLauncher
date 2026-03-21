package dev.turtywurty.veldtlauncher.minecraft.metadata.model;

import java.net.URI;

public record AssetIndex(
        String id,
        String sha1,
        long totalSize,
        URI url,
        long size
) {
}
