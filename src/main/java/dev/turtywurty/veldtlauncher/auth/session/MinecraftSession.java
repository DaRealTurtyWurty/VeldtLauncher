package dev.turtywurty.veldtlauncher.auth.session;

import dev.turtywurty.veldtlauncher.auth.minecraft.MinecraftProfile;

public record MinecraftSession(
        MinecraftProfile profile,
        String accessToken,
        String refreshToken,
        String accountId,
        String clientId
) {
    public MinecraftSession(MinecraftProfile profile, String accessToken, String refreshToken) {
        this(profile, accessToken, refreshToken, null, null);
    }
}
