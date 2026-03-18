package dev.turtywurty.veldtlauncher.auth.microsoft;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public record MicrosoftTokenSet(
        @SerializedName("access_token")
        String accessToken,
        @SerializedName("refresh_token")
        @Nullable String refreshToken,
        @SerializedName("expires_in")
        long expiresIn,
        @SerializedName("token_type")
        String tokenType,
        @SerializedName("scope")
        @Nullable String scope,
        @SerializedName("id_token")
        @Nullable String idToken
) {
}
