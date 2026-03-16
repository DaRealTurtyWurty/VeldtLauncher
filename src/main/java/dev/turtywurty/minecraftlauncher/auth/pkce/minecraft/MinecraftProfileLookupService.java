package dev.turtywurty.minecraftlauncher.auth.pkce.minecraft;

public interface MinecraftProfileLookupService {
    MinecraftProfile getMinecraftProfile(MinecraftAccessToken accessToken);
}
