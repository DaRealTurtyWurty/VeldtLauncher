package dev.turtywurty.veldtlauncher.auth.microsoft;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.turtywurty.veldtlauncher.auth.AuthException;

public final class MicrosoftTokenResponseParser {
    private static final Gson GSON = new Gson();

    private MicrosoftTokenResponseParser() {
    }

    public static MicrosoftTokenResponse parse(String json) throws AuthException {
        if (json == null || json.isBlank())
            throw new AuthException("Response body from Microsoft token endpoint was empty.");

        final JsonObject jsonObject;
        try {
            jsonObject = GSON.fromJson(json, JsonObject.class);
        } catch (JsonParseException exception) {
            throw new AuthException("Failed to parse Microsoft token response JSON.", exception);
        }

        if (jsonObject == null)
            throw new AuthException("Microsoft token response JSON was null.");

        if (jsonObject.has("error")) {
            MicrosoftError error = parseError(jsonObject);
            return MicrosoftTokenResponse.error(error);
        }

        try {
            MicrosoftTokenSet tokenSet = new MicrosoftTokenSet(
                    getString(jsonObject, "access_token"),
                    getString(jsonObject, "refresh_token"),
                    getLong(jsonObject, "expires_in"),
                    getString(jsonObject, "token_type"),
                    getString(jsonObject, "scope"),
                    getString(jsonObject, "id_token")
            );
            return MicrosoftTokenResponse.success(tokenSet);
        } catch (RuntimeException exception) {
            throw new AuthException("Failed to parse Microsoft token set.", exception);
        }
    }

    private static MicrosoftError parseError(JsonObject jsonObject) {
        return new MicrosoftError(
                getString(jsonObject, "error"),
                getString(jsonObject, "error_description"),
                getIntArray(jsonObject, "error_codes"),
                getString(jsonObject, "timestamp"),
                getString(jsonObject, "trace_id"),
                getString(jsonObject, "correlation_id")
        );
    }

    private static String getString(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName))
            return null;

        JsonElement element = jsonObject.get(fieldName);
        if (element == null || element.isJsonNull())
            return null;

        return element.getAsString();
    }

    private static long getLong(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName))
            return 0;

        JsonElement element = jsonObject.get(fieldName);
        if (element == null || element.isJsonNull())
            return 0;

        return element.getAsLong();
    }

    private static int[] getIntArray(JsonObject jsonObject, String fieldName) {
        if (!jsonObject.has(fieldName) || !jsonObject.get(fieldName).isJsonArray())
            return new int[0];

        return GSON.fromJson(jsonObject.get(fieldName), int[].class);
    }
}
