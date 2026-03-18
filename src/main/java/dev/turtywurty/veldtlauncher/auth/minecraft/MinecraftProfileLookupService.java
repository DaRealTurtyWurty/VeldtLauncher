package dev.turtywurty.veldtlauncher.auth.minecraft;

public interface MinecraftProfileLookupService {
    MinecraftProfile lookupProfile(MinecraftAccessToken accessToken);
}
