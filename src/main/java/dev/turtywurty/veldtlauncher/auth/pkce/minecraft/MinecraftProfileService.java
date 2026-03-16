package dev.turtywurty.veldtlauncher.auth.pkce.minecraft;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.turtywurty.veldtlauncher.auth.AuthException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class MinecraftProfileService implements MinecraftProfileLookupService {
    private static final Gson GSON = new Gson();
    private static final String PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    private final HttpClient httpClient;

    public MinecraftProfileService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public MinecraftProfileService() {
        this(HttpClient.newHttpClient());
    }

    public MinecraftProfile getMinecraftProfile(MinecraftAccessToken accessToken) {
        validateAccessToken(accessToken);

        HttpRequest request = HttpRequest.newBuilder(URI.create(PROFILE_URL))
                .header("Accept", "application/json")
                .header("Authorization", accessToken.tokenType() + " " + accessToken.accessToken())
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleProfileResponse(response);
        } catch (AuthException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthException("Interrupted while fetching Minecraft profile.", exception);
        } catch (IOException exception) {
            throw new AuthException("Network error while fetching Minecraft profile.", exception);
        } catch (Exception exception) {
            throw new AuthException("Failed to fetch Minecraft profile.", exception);
        }
    }

    private MinecraftProfile handleProfileResponse(HttpResponse<String> response) {
        JsonObject jsonObject = parseJson(response.body(), "Minecraft profile endpoint");

        if (!jsonObject.has("id") || !jsonObject.has("name"))
            throw new AuthException(buildMinecraftServicesErrorMessage(
                    "Minecraft profile endpoint",
                    response.statusCode(),
                    jsonObject
            ));

        if (response.statusCode() != 200)
            throw new AuthException(
                    "Minecraft profile endpoint returned unexpected HTTP status " + response.statusCode()
            );

        final MinecraftProfile profile;
        try {
            profile = new MinecraftProfile(
                    getString(jsonObject, "id"),
                    getString(jsonObject, "name"),
                    getArray(jsonObject, "skins", MinecraftProfile.Skin[].class),
                    getArray(jsonObject, "capes", MinecraftProfile.Cape[].class)
            );
        } catch (RuntimeException exception) {
            throw new AuthException("Failed to parse Minecraft profile response.", exception);
        }

        validateProfile(profile);
        return profile;
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

    private void validateAccessToken(MinecraftAccessToken accessToken) {
        if (accessToken == null)
            throw new AuthException("Minecraft access token is required to fetch profile.");

        if (isBlank(accessToken.accessToken()))
            throw new AuthException("Minecraft access token value is required to fetch profile.");

        if (isBlank(accessToken.tokenType()))
            throw new AuthException("Minecraft access token type is required to fetch profile.");
    }

    private void validateProfile(MinecraftProfile profile) {
        if (profile == null)
            throw new AuthException("Minecraft profile response did not contain a profile.");

        if (isBlank(profile.id()))
            throw new AuthException("Minecraft profile response did not contain a profile id.");

        if (isBlank(profile.name()))
            throw new AuthException("Minecraft profile response did not contain a profile name.");
    }

    private String buildMinecraftServicesErrorMessage(
            String endpointName,
            int statusCode,
            JsonObject jsonObject
    ) {
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

    private <T> T getArray(JsonObject jsonObject, String fieldName, Class<T> type) {
        if (!jsonObject.has(fieldName) || !jsonObject.get(fieldName).isJsonArray())
            return GSON.fromJson("[]", type);

        return GSON.fromJson(jsonObject.get(fieldName), type);
    }
}
