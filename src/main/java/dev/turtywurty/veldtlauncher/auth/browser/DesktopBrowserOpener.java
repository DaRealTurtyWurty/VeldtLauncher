package dev.turtywurty.veldtlauncher.auth.browser;

import dev.turtywurty.veldtlauncher.auth.AuthException;

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
