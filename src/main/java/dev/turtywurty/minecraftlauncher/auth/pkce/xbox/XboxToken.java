package dev.turtywurty.minecraftlauncher.auth.pkce.xbox;

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
