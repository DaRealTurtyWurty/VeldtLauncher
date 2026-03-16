package dev.turtywurty.veldtlauncher.auth.pkce.microsoft;

import dev.turtywurty.veldtlauncher.auth.AuthException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MicrosoftOAuthClient implements MicrosoftOAuthService {
    private final HttpClient httpClient;

    public MicrosoftOAuthClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public MicrosoftOAuthClient() {
        this(HttpClient.newHttpClient());
    }

    public MicrosoftTokenSet exchangeAuthorizationCode(String clientId, String code, String redirectUri, String codeVerifier) {
        HttpRequest request = new MicrosoftAuthorizationCodeTokenRequestBuilder(
                clientId, code, redirectUri, codeVerifier
        ).build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleTokenResponse(response);
        } catch (AuthException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthException("Interrupted while exchanging authorization code for tokens.", exception);
        } catch (IOException exception) {
            throw new AuthException("Network error while exchanging authorization code for tokens.", exception);
        } catch (Exception exception) {
            throw new AuthException("Failed to exchange authorization code for tokens.", exception);
        }
    }

    public MicrosoftTokenSet refreshAccessToken(
            String clientId,
            String refreshToken
    ) throws AuthException {
        HttpRequest request = new MicrosoftRefreshTokenRequestBuilder(
                clientId,
                refreshToken
        ).build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleTokenResponse(response);
        } catch (AuthException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthException("Interrupted while refreshing Microsoft access token.", exception);
        } catch (IOException exception) {
            throw new AuthException("Network error while refreshing Microsoft access token.", exception);
        } catch (Exception exception) {
            throw new AuthException("Failed to refresh Microsoft access token.", exception);
        }
    }

    private MicrosoftTokenSet handleTokenResponse(HttpResponse<String> response) throws AuthException {
        String responseBody = response.body();
        MicrosoftTokenResponse tokenResponse = MicrosoftTokenResponseParser.parse(responseBody);

        if (tokenResponse.isError()) {
            MicrosoftError error = tokenResponse.error();
            throw new AuthException(buildOAuthErrorMessage(response.statusCode(), error));
        }

        if (!tokenResponse.isSuccess())
            throw new AuthException(
                    "Microsoft token endpoint returned an unrecognized response. HTTP status: " + response.statusCode()
            );

        if (response.statusCode() != 200)
            throw new AuthException(
                    "Microsoft token endpoint returned unexpected HTTP status " + response.statusCode()
            );

        MicrosoftTokenSet tokenSet = tokenResponse.tokenSet();
        validateTokenSet(tokenSet);

        return tokenSet;
    }

    private void validateTokenSet(MicrosoftTokenSet tokenSet) throws AuthException {
        if (tokenSet == null)
            throw new AuthException("Token response did not contain a token set.");

        if (isBlank(tokenSet.accessToken()))
            throw new AuthException("Token response did not contain an access token.");

        if (isBlank(tokenSet.tokenType()))
            throw new AuthException("Token response did not contain a token type.");

        if (tokenSet.expiresIn() <= 0)
            throw new AuthException("Token response contained an invalid expires_in value: " + tokenSet.expiresIn());
    }

    private String buildOAuthErrorMessage(int statusCode, MicrosoftError error) {
        StringBuilder builder = new StringBuilder()
                .append("Microsoft token endpoint returned an OAuth error")
                .append(" (HTTP ").append(statusCode).append(")");

        if (error != null) {
            if (!isBlank(error.error())) {
                builder.append(": ").append(error.error());
            }

            if (!isBlank(error.errorDescription())) {
                builder.append(" - ").append(error.errorDescription());
            }

            if (!isBlank(error.correlationId())) {
                builder.append(" [correlation_id=").append(error.correlationId()).append("]");
            }

            if (!isBlank(error.traceId())) {
                builder.append(" [trace_id=").append(error.traceId()).append("]");
            }
        }

        return builder.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
