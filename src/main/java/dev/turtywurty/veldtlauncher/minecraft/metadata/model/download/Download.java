package dev.turtywurty.veldtlauncher.minecraft.metadata.model.download;

import java.net.URI;

public record Download(
        String sha1,
        long size,
        URI url
) {
}
