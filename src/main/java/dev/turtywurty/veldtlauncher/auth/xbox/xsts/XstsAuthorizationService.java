package dev.turtywurty.veldtlauncher.auth.xbox.xsts;

import dev.turtywurty.veldtlauncher.auth.xbox.XboxToken;

public interface XstsAuthorizationService {
    XstsToken authorize(XboxToken xboxToken);
}
