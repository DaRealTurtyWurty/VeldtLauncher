package dev.turtywurty.veldtlauncher.auth.devicecode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.turtywurty.veldtlauncher.auth.AuthException;
import dev.turtywurty.veldtlauncher.auth.microsoft.MicrosoftError;
import dev.turtywurty.veldtlauncher.util.JsonUtil;

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

        if (JsonUtil.contains(jsonObject, "error")) {
            MicrosoftError error = parseError(jsonObject);
            return MicrosoftDeviceCodeResponse.error(error);
        }

        try {
            MicrosoftDeviceCode deviceCode = new MicrosoftDeviceCode(
                    JsonUtil.getString(jsonObject, "device_code"),
                    JsonUtil.getString(jsonObject, "user_code"),
                    JsonUtil.getString(jsonObject, "verification_uri"),
                    JsonUtil.getString(jsonObject, "verification_uri_complete"),
                    JsonUtil.getLong(jsonObject, "expires_in"),
                    JsonUtil.getLong(jsonObject, "interval"),
                    JsonUtil.getString(jsonObject, "message")
            );
            return MicrosoftDeviceCodeResponse.success(deviceCode);
        } catch (RuntimeException exception) {
            throw new AuthException("Failed to parse Microsoft device code response.", exception);
        }
    }

    private static MicrosoftError parseError(JsonObject jsonObject) {
        return new MicrosoftError(
                JsonUtil.getString(jsonObject, "error"),
                JsonUtil.getString(jsonObject, "error_description"),
                JsonUtil.getIntArray(jsonObject, "error_codes"),
                JsonUtil.getString(jsonObject, "timestamp"),
                JsonUtil.getString(jsonObject, "trace_id"),
                JsonUtil.getString(jsonObject, "correlation_id")
        );
    }
}
