package dev.turtywurty.veldtlauncher.auth;

import java.net.URI;

public final class AuthConfig {
    public static final String DEFAULT_CLIENT_ID = "f37ef7fd-1d43-47e2-84dd-3206110d3b57";
    public static final String DEFAULT_REDIRECT_URI_PATH = "oauth/callback";
    public static final int DEFAULT_REDIRECT_URI_PORT = 43675;

    private AuthConfig() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String getClientId() {
        return System.getenv().getOrDefault("VELDT_AUTH_CLIENT_ID", DEFAULT_CLIENT_ID);
    }

    public static int getRedirectUriPort() {
        String portStr = System.getenv("VELDT_AUTH_REDIRECT_URI_PORT");
        if (portStr != null) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException _) {
                System.err.println("Invalid port number in VELDT_AUTH_REDIRECT_URI_PORT: " + portStr + ". Falling back to default port " + DEFAULT_REDIRECT_URI_PORT);
            }
        }

        return DEFAULT_REDIRECT_URI_PORT;
    }

    public static String getRedirectUriPath() {
        return System.getenv().getOrDefault("VELDT_AUTH_REDIRECT_URI_PATH", DEFAULT_REDIRECT_URI_PATH);
    }

    public static URI getRedirectUri() {
        return URI.create("%s:%d/%s".formatted(
                "http://localhost",
                getRedirectUriPort(),
                getRedirectUriPath()
        ));
    }
}
