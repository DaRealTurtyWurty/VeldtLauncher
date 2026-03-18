package dev.turtywurty.veldtlauncher.auth.microsoft;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public record MicrosoftError(
        @SerializedName("error")
        String error,
        @SerializedName("error_description")
        String errorDescription,
        @SerializedName("error_codes")
        int[] errorCodes,
        @SerializedName("timestamp")
        String timestamp,
        @SerializedName("trace_id")
        @Nullable String traceId,
        @SerializedName("correlation_id")
        @Nullable String correlationId
) {
    public Type getType() {
        try {
            return Type.valueOf(error.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Type.UNKNOWN_ERROR;
        }
    }

    public enum Type {
        AUTHORIZATION_PENDING,
        SLOW_DOWN,
        AUTHORIZATION_DECLINED,
        EXPIRED_TOKEN,
        BAD_VERIFICATION_CODE,
        INVALID_REQUEST,
        INVALID_GRANT,
        UNAUTHORIZED_CLIENT,
        INVALID_CLIENT,
        UNSUPPORTED_GRANT_TYPE,
        INVALID_RESOURCE,
        INTERACTION_REQUIRED,
        TEMPORARILY_UNAVAILABLE,
        CONSENT_REQUIRED,
        INVALID_SCOPE,
        UNKNOWN_ERROR
    }
}
