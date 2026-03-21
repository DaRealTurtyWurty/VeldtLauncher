package dev.turtywurty.veldtlauncher.minecraft.metadata;

import dev.turtywurty.veldtlauncher.minecraft.metadata.model.AssetIndex;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.JavaVersion;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.Library;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument.Arguments;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.download.Downloads;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.logging.Logging;

import java.time.Instant;
import java.util.List;

public record VersionMetadata(
        Arguments arguments,
        AssetIndex assetIndex,
        String assets,
        String complianceLevel,
        Downloads downloads,
        String id,
        JavaVersion javaVersion,
        List<Library> libraries,
        Logging logging,
        String mainClass,
        int minimumLauncherVersion,
        Instant releaseTime,
        Instant time,
        String type
) {
}
