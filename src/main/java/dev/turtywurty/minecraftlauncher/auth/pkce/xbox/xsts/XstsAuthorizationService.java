package dev.turtywurty.minecraftlauncher.auth.pkce.xbox.xsts;

import dev.turtywurty.minecraftlauncher.auth.pkce.xbox.XboxToken;

public interface XstsAuthorizationService {
    XstsToken authorize(XboxToken xboxToken);
}
