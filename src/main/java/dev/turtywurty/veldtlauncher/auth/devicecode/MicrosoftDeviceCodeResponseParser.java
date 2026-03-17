package dev.turtywurty.veldtlauncher.auth.devicecode;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.turtywurty.veldtlauncher.auth.AuthException;
import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftError;

public final class MicrosoftDeviceCodeResponseParser {
    private static final Gson GSON = new Gson();

    private MicrosoftDeviceCodeResponseParser() {
    }

    public static MicrosoftDeviceCodeResponse parse(String json) throws AuthException {
        if (json == null || json.isBlank())
            throw new AuthException("Response body from Microsoft device code endpoint was empty.");

        final JsonObject jsonObject;
        try {
            jsonObject = GSON.fromJson(json, JsonObject.class);
        } catch (JsonParseException exception) {
            throw new AuthException("Failed to parse Microsoft device code response JSON.", exception);
        }

        if (jsonObject == null)
            throw new AuthException("Microsoft device code response JSON was null.");

        if (jsonObject.has("error")) {
            MicrosoftError error = parseError(jsonObject);
            return MicrosoftDeviceCodeResponse.error(error);
        }

        try {
            MicrosoftDeviceCode deviceCode = new MicrosoftDeviceCode(
                    getString(jsonObject, "device_code"),
                    getString(jsonObject, "user_code"),
                    getString(jsonObject, "verification_uri"),
                    getString(jsonObject, "verification_uri_complete"),
                    getLong(jsonObject, "expires_in"),
                    getLong(jsonObject, "interval"),
                    getString(jsonObject, "message")
            );
            return MicrosoftDeviceCodeResponse.success(deviceCode);
        } catch (RuntimeException exception) {
            throw new AuthException("Failed to parse Microsoft device code response.", exception);
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
