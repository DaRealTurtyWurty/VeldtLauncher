package dev.turtywurty.veldtlauncher.minecraft.launch;

import dev.turtywurty.veldtlauncher.auth.session.MinecraftSession;
import dev.turtywurty.veldtlauncher.minecraft.install.InstallResult;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;

import java.io.IOException;

public interface GameLaunchService {
    Process launch(VersionMetadata metadata, InstallResult installResult, MinecraftSession session) throws IOException;
}
