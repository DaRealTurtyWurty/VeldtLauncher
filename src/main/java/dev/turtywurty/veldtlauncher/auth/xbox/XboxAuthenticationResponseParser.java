package dev.turtywurty.veldtlauncher.auth.xbox;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.turtywurty.veldtlauncher.auth.AuthException;
import dev.turtywurty.veldtlauncher.util.JsonUtil;

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

        if (JsonUtil.contains(jsonObject, "Token")) {
            try {
                XboxToken token = new XboxToken(
                        JsonUtil.getString(jsonObject, "IssueInstant"),
                        JsonUtil.getString(jsonObject, "NotAfter"),
                        JsonUtil.getString(jsonObject, "Token"),
                        extractUhs(jsonObject)
                );
                return XboxAuthenticationResponse.success(token);
            } catch (RuntimeException exception) {
                throw new AuthException("Failed to parse Xbox token response.", exception);
            }
        }

        if (JsonUtil.contains(jsonObject, "XErr") || JsonUtil.contains(jsonObject, "Message")) {
            try {
                XboxError error = new XboxError(
                        JsonUtil.getString(jsonObject, "Identity"),
                        JsonUtil.getLongObject(jsonObject, "XErr"),
                        JsonUtil.getString(jsonObject, "Message"),
                        JsonUtil.getString(jsonObject, "Redirect")
                );
                return XboxAuthenticationResponse.error(error);
            } catch (RuntimeException exception) {
                throw new AuthException("Failed to parse Xbox error response.", exception);
            }
        }

        throw new AuthException("Xbox user auth endpoint returned an unrecognized response payload.");
    }

    private static String extractUhs(JsonObject jsonObject) {
        JsonObject displayClaims = JsonUtil.getObject(jsonObject, "DisplayClaims");
        JsonArray xuiArray = JsonUtil.getArray(displayClaims, "xui", null);
        JsonObject xuiObject = JsonUtil.getObject(xuiArray, 0);
        return JsonUtil.getString(xuiObject, "uhs");
    }
}
