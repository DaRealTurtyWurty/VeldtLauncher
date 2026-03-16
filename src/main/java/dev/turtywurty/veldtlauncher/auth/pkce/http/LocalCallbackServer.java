package dev.turtywurty.veldtlauncher.auth.pkce.http;

import dev.turtywurty.veldtlauncher.auth.AuthConfig;
import io.javalin.Javalin;

import java.io.UncheckedIOException;
import java.util.function.Consumer;

public class LocalCallbackServer implements CallbackServer {
    private final int port;
    private final Javalin app;

    private Consumer<AuthCallbackResult> callback;

    public LocalCallbackServer(int port, String path) {
        this.port = port;

        this.app = Javalin.create(config -> {
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
    public void start(Consumer<AuthCallbackResult> callback) {
        if (this.app.jettyServer().started()) {
            this.app.stop();
        }

        this.callback = callback;
        this.app.start(port);
    }

    @Override
    public void stop() {
        this.callback = null;
        this.app.stop();
    }

    @Override
    public boolean isSupported() {
        try {
            this.app.jettyServer().start();
            this.app.jettyServer().stop();
            return true;
        } catch (Exception _) {
            return false;
        }
    }
}
