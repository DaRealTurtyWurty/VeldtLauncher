package dev.turtywurty.veldtlauncher.auth.pkce.microsoft;

public record MicrosoftTokenResponse(
        MicrosoftTokenSet tokenSet,
        MicrosoftError error
) {
    public static MicrosoftTokenResponse success(MicrosoftTokenSet tokenSet) {
        return new MicrosoftTokenResponse(tokenSet, null);
    }

    public static MicrosoftTokenResponse error(MicrosoftError error) {
        return new MicrosoftTokenResponse(null, error);
    }

    public boolean isSuccess() {
        return tokenSet != null;
    }

    public boolean isError() {
        return error != null;
    }
}
