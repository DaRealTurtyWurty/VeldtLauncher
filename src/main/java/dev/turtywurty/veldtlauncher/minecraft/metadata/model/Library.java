package dev.turtywurty.veldtlauncher.minecraft.metadata.model;

import dev.turtywurty.veldtlauncher.minecraft.metadata.model.download.LibraryDownloads;

import java.net.URI;

public record Library(
        LibraryDownloads downloads,
        String name,
        URI url,
        java.util.List<Rule> rules,
        Native natives,
        Extract extract
) {
}
