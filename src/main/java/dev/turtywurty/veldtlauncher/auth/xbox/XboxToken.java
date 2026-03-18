package dev.turtywurty.veldtlauncher.auth.xbox;

import com.google.gson.annotations.SerializedName;

public record XboxToken(
        @SerializedName("IssueInstant")
        String issueInstant,
        @SerializedName("NotAfter")
        String notAfter,
        @SerializedName("Token")
        String token,
        String uhs
) {
}
