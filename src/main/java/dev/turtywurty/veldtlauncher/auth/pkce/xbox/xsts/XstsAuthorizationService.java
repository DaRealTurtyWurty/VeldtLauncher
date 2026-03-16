package dev.turtywurty.veldtlauncher.auth.pkce.xbox.xsts;

import dev.turtywurty.veldtlauncher.auth.pkce.xbox.XboxToken;

public interface XstsAuthorizationService {
    XstsToken authorize(XboxToken xboxToken);
}
