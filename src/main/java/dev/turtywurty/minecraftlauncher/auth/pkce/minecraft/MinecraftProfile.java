package dev.turtywurty.minecraftlauncher.auth.pkce.minecraft;

public record MinecraftProfile(
        String id,
        String name,
        Skin[] skins,
        Cape[] capes
) {
    public record Skin(
            String id,
            String state,
            String url,
            String variant,
            String alias
    ) {
    }

    public record Cape(
            String id,
            String state,
            String url,
            String alias
    ) {
    }
}
