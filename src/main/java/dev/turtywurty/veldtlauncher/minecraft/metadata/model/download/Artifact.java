package dev.turtywurty.veldtlauncher.minecraft.metadata.model.download;

import java.net.URI;

public record Artifact(
        String path,
        String sha1,
        long size,
        URI uri
) {
}
