package dev.turtywurty.minecraftlauncher.auth.pkce.minecraft;

import dev.turtywurty.minecraftlauncher.auth.pkce.xbox.xsts.XstsToken;

public interface MinecraftAuthenticationService {
    MinecraftAccessToken authenticate(XstsToken xstsToken);
}
