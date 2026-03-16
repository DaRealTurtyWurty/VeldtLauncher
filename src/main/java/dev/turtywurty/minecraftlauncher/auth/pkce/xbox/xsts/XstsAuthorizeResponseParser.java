package dev.turtywurty.minecraftlauncher.auth.pkce.xbox.xsts;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import dev.turtywurty.minecraftlauncher.auth.AuthException;

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

        if (jsonObject.has("Token")) {
            try {
                RawXstsToken rawToken = GSON.fromJson(jsonObject, RawXstsToken.class);
                XstsToken token = new XstsToken(
                        rawToken.issueInstant(),
                        rawToken.notAfter(),
                        rawToken.token(),
                        extractUhs(rawToken)
                );
                return XstsAuthorizeResponse.success(token);
            } catch (JsonParseException exception) {
                throw new AuthException("Failed to parse XSTS token response.", exception);
            }
        }

        if (jsonObject.has("XErr") || jsonObject.has("Message")) {
            try {
                XstsError error = GSON.fromJson(jsonObject, XstsError.class);
                return XstsAuthorizeResponse.error(error);
            } catch (JsonParseException exception) {
                throw new AuthException("Failed to parse XSTS error response.", exception);
            }
        }

        throw new AuthException("XSTS authorize endpoint returned an unrecognized response payload.");
    }

    private static String extractUhs(RawXstsToken rawToken) {
        if (rawToken == null || rawToken.displayClaims() == null)
            return null;

        RawXstsToken.DisplayClaims.Xui[] xui = rawToken.displayClaims().xui();
        if (xui == null || xui.length == 0)
            return null;

        return xui[0].uhs();
    }

    private record RawXstsToken(
            @SerializedName("IssueInstant")
            String issueInstant,
            @SerializedName("NotAfter")
            String notAfter,
            @SerializedName("Token")
            String token,
            @SerializedName("DisplayClaims")
            DisplayClaims displayClaims
    ) {
        private record DisplayClaims(
                @SerializedName("xui")
                Xui[] xui
        ) {
            private record Xui(
                    @SerializedName("uhs")
                    String uhs
            ) {
            }
        }
    }
}
