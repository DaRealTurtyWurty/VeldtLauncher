package dev.turtywurty.veldtlauncher.auth.minecraft;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.turtywurty.veldtlauncher.auth.AuthException;
import dev.turtywurty.veldtlauncher.auth.xbox.xsts.XstsToken;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class MinecraftAuthService implements MinecraftAuthenticationService {
    private static final Gson GSON = new Gson();
    private static final String LOGIN_WITH_XBOX_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";

    private final HttpClient httpClient;

    public MinecraftAuthService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public MinecraftAuthService() {
        this(HttpClient.newHttpClient());
    }

    public MinecraftAccessToken authenticate(XstsToken xstsToken) {
        validateXstsToken(xstsToken);

        HttpRequest request = HttpRequest.newBuilder(URI.create(LOGIN_WITH_XBOX_URL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createLoginRequestBody(xstsToken)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleMinecraftAccessTokenResponse(response);
        } catch (AuthException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthException("Interrupted while requesting Minecraft access token.", exception);
        } catch (IOException exception) {
            throw new AuthException("Network error while requesting Minecraft access token.", exception);
        } catch (Exception exception) {
            throw new AuthException("Failed to request Minecraft access token.", exception);
        }
    }

    private MinecraftAccessToken handleMinecraftAccessTokenResponse(HttpResponse<String> response) {
        JsonObject jsonObject = parseJson(
                response.body(),
                "Minecraft login_with_xbox endpoint"
        );

        if (!jsonObject.has("access_token"))
            throw new AuthException(buildMinecraftServicesErrorMessage(
                    "Minecraft login_with_xbox endpoint",
                    response.statusCode(),
                    jsonObject
            ));

        if (response.statusCode() != 200)
            throw new AuthException(
                    "Minecraft login_with_xbox endpoint returned unexpected HTTP status " + response.statusCode()
            );

        final MinecraftAccessToken accessToken;
        try {
            accessToken = new MinecraftAccessToken(
                    getString(jsonObject, "username"),
                    getStringArray(jsonObject, "roles"),
                    getString(jsonObject, "access_token"),
                    getString(jsonObject, "token_type"),
                    getLong(jsonObject, "expires_in")
            );
        } catch (RuntimeException exception) {
            throw new AuthException("Failed to parse Minecraft access token response.", exception);
        }

        validateAccessToken(accessToken);
        return accessToken;
    }

    private JsonObject parseJson(String json, String endpointName) {
        if (json == null || json.isBlank())
            throw new AuthException("Response body from " + endpointName + " was empty.");

        try {
            JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
            if (jsonObject == null)
                throw new AuthException(endpointName + " returned null JSON.");

            return jsonObject;
        } catch (JsonParseException exception) {
            throw new AuthException("Failed to parse JSON from " + endpointName + ".", exception);
        }
    }

    private String createLoginRequestBody(XstsToken xstsToken) {
        JsonObject root = new JsonObject();
        root.addProperty(
                "identityToken",
                "XBL3.0 x=" + xstsToken.uhs() + ";" + xstsToken.token()
        );
        return root.toString();
    }

    private void validateXstsToken(XstsToken xstsToken) {
        if (xstsToken == null)
            throw new AuthException("XSTS token is required to request Minecraft access token.");

        if (isBlank(xstsToken.token()))
            throw new AuthException("XSTS token value is required to request Minecraft access token.");

        if (isBlank(xstsToken.uhs()))
            throw new AuthException("XSTS user hash is required to request Minecraft access token.");
    }

    private void validateAccessToken(MinecraftAccessToken accessToken) {
        if (accessToken == null)
            throw new AuthException("Minecraft access token response did not contain a token.");

        if (isBlank(accessToken.accessToken()))
            throw new AuthException("Minecraft access token response did not contain an access token value.");

        if (isBlank(accessToken.tokenType()))
            throw new AuthException("Minecraft access token response did not contain a token type.");

        if (accessToken.expiresIn() <= 0)
            throw new AuthException(
                    "Minecraft access token response contained an invalid expires_in value: "
                            + accessToken.expiresIn()
            );
    }

    private String buildMinecraftServicesErrorMessage(
            String endpointName,
            int statusCode,
            JsonObject jsonObject
    ) {
        String errorMessage = getString(jsonObject, "errorMessage");
        if ("Minecraft login_with_xbox endpoint".equals(endpointName)
                && errorMessage != null
                && errorMessage.contains("Invalid app registration")) {
            return "This Microsoft app registration is not approved for Minecraft authentication. "
                    + "Use an approved Azure app/client ID for Minecraft, or submit your app for review at "
                    + "https://aka.ms/mce-reviewappid and then retry with that approved client ID.";
        }

        StringBuilder builder = new StringBuilder()
                .append(endpointName)
                .append(" returned an error")
                .append(" (HTTP ").append(statusCode).append(")");

        appendField(builder, jsonObject, "error");
        appendField(builder, jsonObject, "errorMessage");
        appendField(builder, jsonObject, "developerMessage");
        appendField(builder, jsonObject, "path");

        return builder.toString();
    }

    private void appendField(StringBuilder builder, JsonObject jsonObject, String fieldName) {
        if (jsonObject == null || !jsonObject.has(fieldName) || jsonObject.get(fieldName).isJsonNull())
            return;

        String value = jsonObject.get(fieldName).getAsString();
        if (!isBlank(value))
            builder.append(": ").append(fieldName).append('=').append(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String getString(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName))
            return null;

        JsonElement element = jsonObject.get(fieldName);
        if (element == null || element.isJsonNull())
            return null;

        return element.getAsString();
    }

    private long getLong(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName))
            return 0;

        JsonElement element = jsonObject.get(fieldName);
        if (element == null || element.isJsonNull())
            return 0;

        return element.getAsLong();
    }

    private String[] getStringArray(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName) || !jsonObject.get(fieldName).isJsonArray())
            return new String[0];

        return GSON.fromJson(jsonObject.get(fieldName), String[].class);
    }
}
