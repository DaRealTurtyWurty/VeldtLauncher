package dev.turtywurty.veldtlauncher.auth.xbox.xsts;

import com.google.gson.annotations.SerializedName;

public record XstsToken(
        @SerializedName("IssueInstant")
        String issueInstant,
        @SerializedName("NotAfter")
        String notAfter,
        @SerializedName("Token")
        String token,
        String uhs
) {
}
