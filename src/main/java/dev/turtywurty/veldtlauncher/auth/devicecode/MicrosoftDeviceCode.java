package dev.turtywurty.veldtlauncher.auth.devicecode;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public record MicrosoftDeviceCode(
        @SerializedName("device_code")
        String deviceCode,
        @SerializedName("user_code")
        String userCode,
        @SerializedName("verification_uri")
        String verificationUri,
        @SerializedName("verification_uri_complete")
        @Nullable String verificationUriComplete,
        @SerializedName("expires_in")
        long expiresIn,
        @SerializedName("interval")
        long interval,
        @SerializedName("message")
        @Nullable String message
) {
}
