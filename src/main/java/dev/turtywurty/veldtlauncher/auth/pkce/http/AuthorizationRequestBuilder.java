package dev.turtywurty.veldtlauncher.auth.pkce.http;

import dev.turtywurty.veldtlauncher.auth.pkce.PkceValues;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AuthorizationRequestBuilder {
    private final String baseUrl;
    private final String clientId;
    private final String responseType;
    private final URI redirectUri;
    private final List<String> scopes;
    private final PkceValues pkceValues;
    private final String state;

    public AuthorizationRequestBuilder(String clientId, URI redirectUri, PkceValues pkceValues, String state) {
        this(clientId, "code", redirectUri, List.of("XboxLive.signin", "offline_access"), pkceValues, state);
    }

    public AuthorizationRequestBuilder(String clientId, String responseType, URI redirectUri, List<String> scopes, PkceValues pkceValues, String state) {
        this("https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize", clientId, responseType, redirectUri, scopes, pkceValues, state);
    }

    public AuthorizationRequestBuilder(String baseUrl, String clientId, String responseType, URI redirectUri, List<String> scopes, PkceValues pkceValues, String state) {
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.responseType = responseType;
        this.redirectUri = redirectUri;
        this.scopes = scopes;
        this.pkceValues = pkceValues;
        this.state = state;
    }


    public URI build() {
        return URI.create(baseUrl +
                "?client_id=" + uriEncode(clientId) +
                "&response_type=" + uriEncode(responseType) +
                "&redirect_uri=" + uriEncode(String.valueOf(redirectUri)) +
                "&scope=" + uriEncode(String.join(" ", scopes)) +
                "&code_challenge=" + uriEncode(pkceValues.codeChallenge()) +
                "&code_challenge_method=" + uriEncode(pkceValues.codeChallengeMethod()) +
                "&state=" + uriEncode(state));
    }

    private String uriEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
