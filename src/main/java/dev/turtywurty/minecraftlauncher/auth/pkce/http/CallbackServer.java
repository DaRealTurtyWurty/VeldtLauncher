package dev.turtywurty.minecraftlauncher.auth.pkce.http;

import java.util.function.Consumer;

public interface CallbackServer {
    void start(Consumer<AuthCallbackResult> callback);

    void stop();

    boolean isSupported();
}
