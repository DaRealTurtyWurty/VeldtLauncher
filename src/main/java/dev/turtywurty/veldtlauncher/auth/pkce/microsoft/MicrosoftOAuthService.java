package dev.turtywurty.veldtlauncher.auth.pkce.microsoft;

public interface MicrosoftOAuthService {
    MicrosoftTokenSet exchangeAuthorizationCode(String clientId, String code, String redirectUri, String codeVerifier);

    MicrosoftTokenSet refreshAccessToken(String clientId, String refreshToken);
}
