package dev.turtywurty.minecraftlauncher.auth.pkce.microsoft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.turtywurty.minecraftlauncher.auth.AuthException;

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
            MicrosoftError error = GSON.fromJson(jsonObject, MicrosoftError.class);
            return MicrosoftTokenResponse.error(error);
        }

        try {
            MicrosoftTokenSet tokenSet = GSON.fromJson(jsonObject, MicrosoftTokenSet.class);
            return MicrosoftTokenResponse.success(tokenSet);
        } catch (JsonParseException exception) {
            throw new AuthException("Failed to parse Microsoft token set.", exception);
        }
    }
}