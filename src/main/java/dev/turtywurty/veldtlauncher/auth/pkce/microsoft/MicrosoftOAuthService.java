package dev.turtywurty.veldtlauncher.auth.pkce.microsoft;

import dev.turtywurty.veldtlauncher.auth.microsoft.MicrosoftTokenSet;

public interface MicrosoftOAuthService {
    MicrosoftTokenSet exchangeAuthorizationCode(String clientId, String code, String redirectUri, String codeVerifier);

    MicrosoftTokenSet refreshAccessToken(String clientId, String refreshToken);
}
