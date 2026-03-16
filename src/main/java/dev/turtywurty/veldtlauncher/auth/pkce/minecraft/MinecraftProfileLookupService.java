package dev.turtywurty.veldtlauncher.auth.pkce.minecraft;

public interface MinecraftProfileLookupService {
    MinecraftProfile getMinecraftProfile(MinecraftAccessToken accessToken);
}
