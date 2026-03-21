package dev.turtywurty.veldtlauncher.auth.minecraft;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.turtywurty.veldtlauncher.auth.AuthException;
import dev.turtywurty.veldtlauncher.util.JsonUtil;

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

    public MinecraftProfile lookupProfile(MinecraftAccessToken accessToken) {
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

        if (!JsonUtil.contains(jsonObject, "id") || !JsonUtil.contains(jsonObject, "name"))
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
                    JsonUtil.getString(jsonObject, "id"),
                    JsonUtil.getString(jsonObject, "name"),
                    getSkins(jsonObject, "skins"),
                    getCapes(jsonObject, "capes")
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
        String value = JsonUtil.getString(jsonObject, fieldName);
        if (!isBlank(value))
            builder.append(": ").append(fieldName).append('=').append(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private MinecraftProfile.Skin[] getSkins(JsonObject jsonObject, String fieldName) {
        JsonArray array = JsonUtil.getArray(jsonObject, fieldName);
        MinecraftProfile.Skin[] skins = new MinecraftProfile.Skin[array.size()];

        for (int index = 0; index < array.size(); index++) {
            JsonObject skinObject = JsonUtil.getObject(array, index, new JsonObject());
            skins[index] = new MinecraftProfile.Skin(
                    JsonUtil.getString(skinObject, "id"),
                    JsonUtil.getString(skinObject, "state"),
                    JsonUtil.getString(skinObject, "url"),
                    JsonUtil.getString(skinObject, "variant"),
                    JsonUtil.getString(skinObject, "alias")
            );
        }

        return skins;
    }

    private MinecraftProfile.Cape[] getCapes(JsonObject jsonObject, String fieldName) {
        JsonArray array = JsonUtil.getArray(jsonObject, fieldName);
        MinecraftProfile.Cape[] capes = new MinecraftProfile.Cape[array.size()];

        for (int index = 0; index < array.size(); index++) {
            JsonObject capeObject = JsonUtil.getObject(array, index, new JsonObject());
            capes[index] = new MinecraftProfile.Cape(
                    JsonUtil.getString(capeObject, "id"),
                    JsonUtil.getString(capeObject, "state"),
                    JsonUtil.getString(capeObject, "url"),
                    JsonUtil.getString(capeObject, "alias")
            );
        }

        return capes;
    }
}
