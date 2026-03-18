package dev.turtywurty.veldtlauncher.auth.minecraft;

import dev.turtywurty.veldtlauncher.auth.xbox.xsts.XstsToken;

public interface MinecraftAuthenticationService {
    MinecraftAccessToken authenticate(XstsToken xstsToken);
}
