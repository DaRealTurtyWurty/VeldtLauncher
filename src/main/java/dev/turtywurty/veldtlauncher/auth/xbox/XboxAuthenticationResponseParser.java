package dev.turtywurty.veldtlauncher.auth.xbox;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.turtywurty.veldtlauncher.auth.AuthException;

public final class XboxAuthenticationResponseParser {
    private static final Gson GSON = new Gson();

    private XboxAuthenticationResponseParser() {
    }

    public static XboxAuthenticationResponse parse(String json) throws AuthException {
        if (json == null || json.isBlank())
            throw new AuthException("Response body from Xbox user auth endpoint was empty.");

        final JsonObject jsonObject;
        try {
            jsonObject = GSON.fromJson(json, JsonObject.class);
        } catch (JsonParseException exception) {
            throw new AuthException("Failed to parse Xbox user auth response JSON.", exception);
        }

        if (jsonObject == null)
            throw new AuthException("Xbox user auth response JSON was null.");

        if (jsonObject.has("Token")) {
            try {
                XboxToken token = new XboxToken(
                        getString(jsonObject, "IssueInstant"),
                        getString(jsonObject, "NotAfter"),
                        getString(jsonObject, "Token"),
                        extractUhs(jsonObject)
                );
                return XboxAuthenticationResponse.success(token);
            } catch (RuntimeException exception) {
                throw new AuthException("Failed to parse Xbox token response.", exception);
            }
        }

        if (jsonObject.has("XErr") || jsonObject.has("Message")) {
            try {
                XboxError error = new XboxError(
                        getString(jsonObject, "Identity"),
                        getLongObject(jsonObject, "XErr"),
                        getString(jsonObject, "Message"),
                        getString(jsonObject, "Redirect")
                );
                return XboxAuthenticationResponse.error(error);
            } catch (RuntimeException exception) {
                throw new AuthException("Failed to parse Xbox error response.", exception);
            }
        }

        throw new AuthException("Xbox user auth endpoint returned an unrecognized response payload.");
    }

    private static String extractUhs(JsonObject jsonObject) {
        if (jsonObject == null || !jsonObject.has("DisplayClaims"))
            return null;

        JsonObject displayClaims = getObject(jsonObject, "DisplayClaims");
        if (displayClaims == null || !displayClaims.has("xui") || !displayClaims.get("xui").isJsonArray())
            return null;

        var xuiArray = displayClaims.getAsJsonArray("xui");
        if (xuiArray.isEmpty() || !xuiArray.get(0).isJsonObject())
            return null;

        return getString(xuiArray.get(0).getAsJsonObject(), "uhs");
    }

    private static JsonObject getObject(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName) || !jsonObject.get(fieldName).isJsonObject())
            return null;

        return jsonObject.getAsJsonObject(fieldName);
    }

    private static String getString(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName))
            return null;

        JsonElement element = jsonObject.get(fieldName);
        if (element == null || element.isJsonNull())
            return null;

        return element.getAsString();
    }

    private static Long getLongObject(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName))
            return null;

        JsonElement element = jsonObject.get(fieldName);
        if (element == null || element.isJsonNull())
            return null;

        return element.getAsLong();
    }
}
