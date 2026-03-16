package dev.turtywurty.veldtlauncher.auth.pkce.microsoft;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class MicrosoftRefreshTokenRequestBuilder {
    private static final String DEFAULT_TOKEN_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";

    private final String tokenUrl;
    private final String clientId;
    private final String refreshToken;
    private final List<String> scopes;

    public MicrosoftRefreshTokenRequestBuilder(
            String clientId,
            String refreshToken
    ) {
        this(DEFAULT_TOKEN_URL, clientId, refreshToken, List.of());
    }

    public MicrosoftRefreshTokenRequestBuilder(
            String clientId,
            String refreshToken,
            List<String> scopes
    ) {
        this(DEFAULT_TOKEN_URL, clientId, refreshToken, scopes);
    }

    public MicrosoftRefreshTokenRequestBuilder(
            String tokenUrl,
            String clientId,
            String refreshToken,
            List<String> scopes
    ) {
        this.tokenUrl = Objects.requireNonNull(tokenUrl, "tokenUrl");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.refreshToken = Objects.requireNonNull(refreshToken, "refreshToken");
        this.scopes = Objects.requireNonNull(scopes, "scopes");
    }

    public HttpRequest build() {
        StringJoiner form = new StringJoiner("&")
                .add("client_id=" + formEncode(clientId))
                .add("grant_type=" + formEncode("refresh_token"))
                .add("refresh_token=" + formEncode(refreshToken));

        if (!scopes.isEmpty()) {
            form.add("scope=" + formEncode(String.join(" ", scopes)));
        }

        return HttpRequest.newBuilder(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                .build();
    }

    private String formEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}