package dev.turtywurty.veldtlauncher.minecraft.metadata.model.logging;

import java.net.URI;

public record LoggingFile(
        String id,
        String sha1,
        long size,
        URI url
) {
}
