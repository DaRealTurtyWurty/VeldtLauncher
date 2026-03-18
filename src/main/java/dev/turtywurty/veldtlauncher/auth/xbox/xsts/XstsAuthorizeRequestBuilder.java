package dev.turtywurty.veldtlauncher.auth.xbox.xsts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Objects;

public class XstsAuthorizeRequestBuilder {
    private static final String DEFAULT_AUTH_URL =
            "https://xsts.auth.xboxlive.com/xsts/authorize";

    private final String authUrl;
    private final String userToken;

    public XstsAuthorizeRequestBuilder(String userToken) {
        this(DEFAULT_AUTH_URL, userToken);
    }

    public XstsAuthorizeRequestBuilder(String authUrl, String userToken) {
        this.authUrl = Objects.requireNonNull(authUrl, "authUrl");
        this.userToken = Objects.requireNonNull(userToken, "userToken");
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
        properties.addProperty("SandboxId", "RETAIL");

        JsonArray userTokens = new JsonArray();
        userTokens.add(userToken);
        properties.add("UserTokens", userTokens);

        root.add("Properties", properties);
        root.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        root.addProperty("TokenType", "JWT");
        return root.toString();
    }
}
