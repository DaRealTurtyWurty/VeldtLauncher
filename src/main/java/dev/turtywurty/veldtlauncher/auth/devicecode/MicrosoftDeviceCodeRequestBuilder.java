package dev.turtywurty.veldtlauncher.auth.devicecode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class MicrosoftDeviceCodeRequestBuilder {
    private static final String DEFAULT_DEVICE_CODE_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";

    private final String deviceCodeUrl;
    private final String clientId;
    private final List<String> scopes;

    public MicrosoftDeviceCodeRequestBuilder(String clientId, List<String> scopes) {
        this(DEFAULT_DEVICE_CODE_URL, clientId, scopes);
    }

    public MicrosoftDeviceCodeRequestBuilder(String deviceCodeUrl, String clientId, List<String> scopes) {
        this.deviceCodeUrl = Objects.requireNonNull(deviceCodeUrl, "deviceCodeUrl");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.scopes = Objects.requireNonNull(scopes, "scopes");
    }

    public HttpRequest build() {
        StringJoiner form = new StringJoiner("&")
                .add("client_id=" + formEncode(clientId));

        if (!scopes.isEmpty()) {
            form.add("scope=" + formEncode(String.join(" ", scopes)));
        }

        return HttpRequest.newBuilder(URI.create(deviceCodeUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                .build();
    }

    private String formEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
