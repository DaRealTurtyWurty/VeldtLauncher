package dev.turtywurty.veldtlauncher.auth.xbox.xsts;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.turtywurty.veldtlauncher.auth.AuthException;
import dev.turtywurty.veldtlauncher.util.JsonUtil;

public final class XstsAuthorizeResponseParser {
    private static final Gson GSON = new Gson();

    private XstsAuthorizeResponseParser() {
    }

    public static XstsAuthorizeResponse parse(String json) throws AuthException {
        if (json == null || json.isBlank())
            throw new AuthException("Response body from XSTS authorize endpoint was empty.");

        final JsonObject jsonObject;
        try {
            jsonObject = GSON.fromJson(json, JsonObject.class);
        } catch (JsonParseException exception) {
            throw new AuthException("Failed to parse XSTS authorize response JSON.", exception);
        }

        if (jsonObject == null)
            throw new AuthException("XSTS authorize response JSON was null.");

        if (JsonUtil.contains(jsonObject, "Token")) {
            try {
                XstsToken token = new XstsToken(
                        JsonUtil.getString(jsonObject, "IssueInstant"),
                        JsonUtil.getString(jsonObject, "NotAfter"),
                        JsonUtil.getString(jsonObject, "Token"),
                        extractUhs(jsonObject)
                );
                return XstsAuthorizeResponse.success(token);
            } catch (RuntimeException exception) {
                throw new AuthException("Failed to parse XSTS token response.", exception);
            }
        }

        if (JsonUtil.contains(jsonObject, "XErr") || JsonUtil.contains(jsonObject, "Message")) {
            try {
                XstsError error = new XstsError(
                        JsonUtil.getString(jsonObject, "Identity"),
                        JsonUtil.getLongObject(jsonObject, "XErr"),
                        JsonUtil.getString(jsonObject, "Message"),
                        JsonUtil.getString(jsonObject, "Redirect")
                );
                return XstsAuthorizeResponse.error(error);
            } catch (RuntimeException exception) {
                throw new AuthException("Failed to parse XSTS error response.", exception);
            }
        }

        throw new AuthException("XSTS authorize endpoint returned an unrecognized response payload.");
    }

    private static String extractUhs(JsonObject jsonObject) {
        JsonObject displayClaims = JsonUtil.getObject(jsonObject, "DisplayClaims");
        JsonArray xuiArray = JsonUtil.getArray(displayClaims, "xui", null);
        JsonObject xuiObject = JsonUtil.getObject(xuiArray, 0);
        return JsonUtil.getString(xuiObject, "uhs");
    }
}
