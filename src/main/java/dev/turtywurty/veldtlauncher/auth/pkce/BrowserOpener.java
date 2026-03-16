package dev.turtywurty.veldtlauncher.auth.pkce;

import java.net.URI;

public interface BrowserOpener {
    void open(URI uri);

    boolean isSupported();
}
