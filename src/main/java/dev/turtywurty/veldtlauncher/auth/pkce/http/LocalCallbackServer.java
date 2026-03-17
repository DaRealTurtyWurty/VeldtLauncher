package dev.turtywurty.veldtlauncher.auth.pkce.http;

import dev.turtywurty.veldtlauncher.auth.AuthConfig;
import io.javalin.Javalin;

import java.io.UncheckedIOException;
import java.util.function.Consumer;

public class LocalCallbackServer implements CallbackServer {
    private final int port;
    private final String path;

    private volatile Javalin app;
    private volatile Consumer<AuthCallbackResult> callback;

    public LocalCallbackServer(int port, String path) {
        this.port = port;
        this.path = normalizePath(path);
    }

    private Javalin createApp() {
        return Javalin.create(config -> {
            config.startup.showJavalinBanner = false;
            config.startup.showOldJavalinVersionWarning = false;

            config.routes.get(path, ctx -> {
                String code = ctx.queryParam("code");
                String state = ctx.queryParam("state");

                AuthCallbackResult result;
                if (code == null || state == null) {
                    String error = ctx.queryParam("error");
                    String errorDescription = ctx.queryParam("error_description");
                    result = AuthCallbackResult.error(error, errorDescription);
                } else {
                    result = AuthCallbackResult.success(code, state);
                }

                ctx.status(200);
                ctx.contentType("text/plain");
                ctx.result("Authentication successful! You can close this window.");

                try {
                    ctx.res().flushBuffer();
                } catch (java.io.IOException exception) {
                    throw new UncheckedIOException("Failed to flush authentication callback response.", exception);
                }

                Consumer<AuthCallbackResult> callback = this.callback;
                if (callback != null) {
                    Thread.startVirtualThread(() -> callback.accept(result));
                }
            });
        });
    }

    public LocalCallbackServer() {
        this(AuthConfig.getRedirectUriPort(), AuthConfig.getRedirectUriPath());
    }

    @Override
    public synchronized void start(Consumer<AuthCallbackResult> callback) {
        stop();
        this.callback = callback;
        Javalin app = createApp();
        this.app = app;
        app.start(port);
    }

    @Override
    public synchronized void stop() {
        this.callback = null;
        Javalin app = this.app;
        this.app = null;
        if (app != null) {
            app.stop();
        }
    }

    @Override
    public boolean isSupported() {
        try {
            Javalin app = createApp();
            app.start(port);
            app.stop();
            return true;
        } catch (Exception _) {
            return false;
        }
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("Callback path must not be blank.");

        return path.startsWith("/") ? path : "/" + path;
    }
}
