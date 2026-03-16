package dev.turtywurty.veldtlauncher.auth.pkce.xbox;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public record XboxError(
        @SerializedName("Identity")
        @Nullable String identity,
        @SerializedName("XErr")
        @Nullable Long xErr,
        @SerializedName("Message")
        @Nullable String message,
        @SerializedName("Redirect")
        @Nullable String redirect
) {
}
