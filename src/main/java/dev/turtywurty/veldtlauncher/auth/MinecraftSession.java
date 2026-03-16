package dev.turtywurty.veldtlauncher.auth;

import dev.turtywurty.veldtlauncher.auth.pkce.minecraft.MinecraftProfile;

public record MinecraftSession(
        MinecraftProfile profile,
        String accessToken,
        String refreshToken
) {}