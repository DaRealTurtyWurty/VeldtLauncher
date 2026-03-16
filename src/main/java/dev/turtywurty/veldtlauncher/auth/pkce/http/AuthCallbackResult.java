package dev.turtywurty.veldtlauncher.auth.pkce.http;

public record AuthCallbackResult(String code, String state, String error, String errorDescription) {
    public static AuthCallbackResult success(String code, String state) {
        return new AuthCallbackResult(code, state, null, null);
    }

    public static AuthCallbackResult error(String error, String errorDescription) {
        return new AuthCallbackResult(null, null, error, errorDescription);
    }

    public boolean isSuccess() {
        return code != null && state != null;
    }

    public boolean isError() {
        return error != null;
    }
}
