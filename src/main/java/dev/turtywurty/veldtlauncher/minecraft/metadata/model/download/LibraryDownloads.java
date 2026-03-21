package dev.turtywurty.veldtlauncher.minecraft.metadata.model.download;

import java.util.Map;

public record LibraryDownloads(
        Artifact artifact,
        Map<String, Artifact> classifiers
) {
}
