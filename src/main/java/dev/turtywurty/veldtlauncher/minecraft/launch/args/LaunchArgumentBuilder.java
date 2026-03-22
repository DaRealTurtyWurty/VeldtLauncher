package dev.turtywurty.veldtlauncher.minecraft.launch.args;

import dev.turtywurty.veldtlauncher.auth.session.MinecraftSession;
import dev.turtywurty.veldtlauncher.minecraft.install.InstallResult;
import dev.turtywurty.veldtlauncher.minecraft.java.JDK;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;

public interface LaunchArgumentBuilder {
    LaunchArguments build(
            VersionMetadata metadata,
            InstallResult installResult,
            MinecraftSession session,
            JDK jdk
    );
}
