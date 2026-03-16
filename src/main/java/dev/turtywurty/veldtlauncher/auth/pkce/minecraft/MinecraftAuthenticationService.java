package dev.turtywurty.veldtlauncher.auth.pkce.minecraft;

import dev.turtywurty.veldtlauncher.auth.pkce.xbox.xsts.XstsToken;

public interface MinecraftAuthenticationService {
    MinecraftAccessToken authenticate(XstsToken xstsToken);
}
