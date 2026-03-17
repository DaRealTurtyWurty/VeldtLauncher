package dev.turtywurty.veldtlauncher.auth.devicecode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.StringJoiner;

public class MicrosoftDeviceCodeTokenRequestBuilder {
    private static final String DEFAULT_TOKEN_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String DEVICE_CODE_GRANT_TYPE =
            "urn:ietf:params:oauth:grant-type:device_code";

    private final String tokenUrl;
    private final String clientId;
    private final String deviceCode;

    public MicrosoftDeviceCodeTokenRequestBuilder(String clientId, String deviceCode) {
        this(DEFAULT_TOKEN_URL, clientId, deviceCode);
    }

    public MicrosoftDeviceCodeTokenRequestBuilder(String tokenUrl, String clientId, String deviceCode) {
        this.tokenUrl = Objects.requireNonNull(tokenUrl, "tokenUrl");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.deviceCode = Objects.requireNonNull(deviceCode, "deviceCode");
    }

    public HttpRequest build() {
        String form = new StringJoiner("&")
                .add("client_id=" + formEncode(clientId))
                .add("grant_type=" + formEncode(DEVICE_CODE_GRANT_TYPE))
                .add("device_code=" + formEncode(deviceCode))
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
