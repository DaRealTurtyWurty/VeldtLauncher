package dev.turtywurty.minecraftlauncher.auth.pkce;

import dev.turtywurty.minecraftlauncher.auth.AuthException;

import java.awt.*;
import java.net.URI;

public final class DesktopBrowserOpener implements BrowserOpener {
    @Override
    public void open(URI uri) {
        try {
            Desktop.getDesktop().browse(uri);
        } catch (Exception exception) {
            throw new AuthException("Failed to open browser for authentication.", exception);
        }
    }

    @Override
    public boolean isSupported() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }
}
