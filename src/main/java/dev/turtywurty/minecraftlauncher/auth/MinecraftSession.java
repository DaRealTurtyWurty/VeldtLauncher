package dev.turtywurty.minecraftlauncher.auth;

import dev.turtywurty.minecraftlauncher.auth.pkce.minecraft.MinecraftProfile;

public record MinecraftSession(
        MinecraftProfile profile,
        String accessToken,
        String refreshToken
) {}