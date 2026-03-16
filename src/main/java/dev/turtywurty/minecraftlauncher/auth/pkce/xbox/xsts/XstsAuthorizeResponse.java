package dev.turtywurty.minecraftlauncher.auth.pkce.xbox.xsts;

public record XstsAuthorizeResponse(
        XstsToken token,
        XstsError error
) {
    public static XstsAuthorizeResponse success(XstsToken token) {
        return new XstsAuthorizeResponse(token, null);
    }

    public static XstsAuthorizeResponse error(XstsError error) {
        return new XstsAuthorizeResponse(null, error);
    }

    public boolean isSuccess() {
        return token != null;
    }

    public boolean isError() {
        return error != null;
    }
}
