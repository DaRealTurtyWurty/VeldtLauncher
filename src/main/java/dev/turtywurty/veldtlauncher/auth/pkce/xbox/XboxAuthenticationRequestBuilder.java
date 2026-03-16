package dev.turtywurty.veldtlauncher.auth.pkce.xbox;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Objects;

public class XboxAuthenticationRequestBuilder {
    private static final String DEFAULT_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";

    private final String authUrl;
    private final String accessToken;

    public XboxAuthenticationRequestBuilder(String accessToken) {
        this(DEFAULT_AUTH_URL, accessToken);
    }

    public XboxAuthenticationRequestBuilder(String authUrl, String accessToken) {
        this.authUrl = Objects.requireNonNull(authUrl, "authUrl");
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken");
    }

    public HttpRequest build() {
        return HttpRequest.newBuilder(URI.create(authUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("x-xbl-contract-version", "1")
                .POST(HttpRequest.BodyPublishers.ofString(createRequestBody()))
                .build();
    }

    private String createRequestBody() {
        JsonObject root = new JsonObject();
        JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", formatRpsTicket(accessToken));
        root.add("Properties", properties);
        root.addProperty("RelyingParty", "http://auth.xboxlive.com");
        root.addProperty("TokenType", "JWT");
        return root.toString();
    }

    private String formatRpsTicket(String accessToken) {
        return accessToken.startsWith("d=") ? accessToken : "d=" + accessToken;
    }
}
