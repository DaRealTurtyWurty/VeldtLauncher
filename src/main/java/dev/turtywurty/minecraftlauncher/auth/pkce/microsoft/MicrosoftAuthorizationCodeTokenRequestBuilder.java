package dev.turtywurty.minecraftlauncher.auth.pkce.microsoft;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.StringJoiner;

public class MicrosoftAuthorizationCodeTokenRequestBuilder {
    private static final String DEFAULT_TOKEN_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";

    private final String tokenUrl;
    private final String clientId;
    private final String code;
    private final String redirectUri;
    private final String codeVerifier;

    public MicrosoftAuthorizationCodeTokenRequestBuilder(
            String clientId,
            String code,
            String redirectUri,
            String codeVerifier
    ) {
        this(DEFAULT_TOKEN_URL, clientId, code, redirectUri, codeVerifier);
    }

    public MicrosoftAuthorizationCodeTokenRequestBuilder(
            String tokenUrl,
            String clientId,
            String code,
            String redirectUri,
            String codeVerifier
    ) {
        this.tokenUrl = Objects.requireNonNull(tokenUrl, "tokenUrl");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.code = Objects.requireNonNull(code, "code");
        this.redirectUri = Objects.requireNonNull(redirectUri, "redirectUri");
        this.codeVerifier = Objects.requireNonNull(codeVerifier, "codeVerifier");
    }

    public HttpRequest build() {
        String form = new StringJoiner("&")
                .add("client_id=" + formEncode(clientId))
                .add("grant_type=" + formEncode("authorization_code"))
                .add("code=" + formEncode(code))
                .add("redirect_uri=" + formEncode(redirectUri))
                .add("code_verifier=" + formEncode(codeVerifier))
                .toString();

        return HttpRequest.newBuilder(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
    }

    private String formEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}