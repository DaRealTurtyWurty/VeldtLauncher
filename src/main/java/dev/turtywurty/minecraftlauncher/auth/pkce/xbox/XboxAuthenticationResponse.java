package dev.turtywurty.minecraftlauncher.auth.pkce.xbox;

public record XboxAuthenticationResponse(
        XboxToken token,
        XboxError error
) {
    public static XboxAuthenticationResponse success(XboxToken token) {
        return new XboxAuthenticationResponse(token, null);
    }

    public static XboxAuthenticationResponse error(XboxError error) {
        return new XboxAuthenticationResponse(null, error);
    }

    public boolean isSuccess() {
        return token != null;
    }

    public boolean isError() {
        return error != null;
    }
}
