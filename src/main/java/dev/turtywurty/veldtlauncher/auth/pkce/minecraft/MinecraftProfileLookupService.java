package dev.turtywurty.veldtlauncher.auth.pkce.minecraft;

public interface MinecraftProfileLookupService {
    MinecraftProfile lookupProfile(MinecraftAccessToken accessToken);
}
