package dev.turtywurty.veldtlauncher.auth.microsoft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.turtywurty.veldtlauncher.auth.AuthException;
import dev.turtywurty.veldtlauncher.util.JsonUtil;

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

        if (JsonUtil.contains(jsonObject, "error")) {
            MicrosoftError error = parseError(jsonObject);
            return MicrosoftTokenResponse.error(error);
        }

        try {
            MicrosoftTokenSet tokenSet = new MicrosoftTokenSet(
                    JsonUtil.getString(jsonObject, "access_token"),
                    JsonUtil.getString(jsonObject, "refresh_token"),
                    JsonUtil.getLong(jsonObject, "expires_in"),
                    JsonUtil.getString(jsonObject, "token_type"),
                    JsonUtil.getString(jsonObject, "scope"),
                    JsonUtil.getString(jsonObject, "id_token")
            );
            return MicrosoftTokenResponse.success(tokenSet);
        } catch (RuntimeException exception) {
            throw new AuthException("Failed to parse Microsoft token set.", exception);
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
