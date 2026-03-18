package dev.turtywurty.veldtlauncher.auth.minecraft;

import com.google.gson.annotations.SerializedName;

public record MinecraftAccessToken(
        @SerializedName("username")
        String username,
        @SerializedName("roles")
        String[] roles,
        @SerializedName("access_token")
        String accessToken,
        @SerializedName("token_type")
        String tokenType,
        @SerializedName("expires_in")
        long expiresIn
) {
}
