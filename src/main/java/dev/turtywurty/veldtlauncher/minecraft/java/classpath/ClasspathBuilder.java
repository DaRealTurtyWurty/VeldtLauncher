package dev.turtywurty.veldtlauncher.minecraft.java.classpath;

import dev.turtywurty.veldtlauncher.minecraft.install.InstallResult;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;

public interface ClasspathBuilder {
    String buildClasspath(VersionMetadata metadata, InstallResult installResult);
}
