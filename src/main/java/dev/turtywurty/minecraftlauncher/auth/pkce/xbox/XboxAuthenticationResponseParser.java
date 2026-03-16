package dev.turtywurty.minecraftlauncher.auth.pkce.xbox;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import dev.turtywurty.minecraftlauncher.auth.AuthException;

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
                RawXboxToken rawToken = GSON.fromJson(jsonObject, RawXboxToken.class);
                XboxToken token = new XboxToken(
                        rawToken.issueInstant(),
                        rawToken.notAfter(),
                        rawToken.token(),
                        extractUhs(rawToken)
                );
                return XboxAuthenticationResponse.success(token);
            } catch (JsonParseException exception) {
                throw new AuthException("Failed to parse Xbox token response.", exception);
            }
        }

        if (jsonObject.has("XErr") || jsonObject.has("Message")) {
            try {
                XboxError error = GSON.fromJson(jsonObject, XboxError.class);
                return XboxAuthenticationResponse.error(error);
            } catch (JsonParseException exception) {
                throw new AuthException("Failed to parse Xbox error response.", exception);
            }
        }

        throw new AuthException("Xbox user auth endpoint returned an unrecognized response payload.");
    }

    private static String extractUhs(RawXboxToken rawToken) {
        if (rawToken == null || rawToken.displayClaims() == null)
            return null;

        RawXboxToken.DisplayClaims.Xui[] xui = rawToken.displayClaims().xui();
        if (xui == null || xui.length == 0)
            return null;

        return xui[0].uhs();
    }

    private record RawXboxToken(
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
