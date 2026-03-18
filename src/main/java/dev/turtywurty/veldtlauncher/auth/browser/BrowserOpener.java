package dev.turtywurty.veldtlauncher.auth.browser;

import java.net.URI;

public interface BrowserOpener {
    void open(URI uri);

    boolean isSupported();
}
