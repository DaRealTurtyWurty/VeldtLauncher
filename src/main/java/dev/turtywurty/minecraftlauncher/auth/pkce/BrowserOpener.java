package dev.turtywurty.minecraftlauncher.auth.pkce;

import java.net.URI;

public interface BrowserOpener {
    void open(URI uri);

    boolean isSupported();
}
