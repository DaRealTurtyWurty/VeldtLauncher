package dev.turtywurty.veldtlauncher.auth.pkce.xbox.xsts;

import dev.turtywurty.veldtlauncher.auth.AuthException;
import dev.turtywurty.veldtlauncher.auth.pkce.xbox.XboxAuthErrorMessages;
import dev.turtywurty.veldtlauncher.auth.pkce.xbox.XboxToken;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

public class XstsAuthService implements XstsAuthorizationService {
    private final HttpClient httpClient;

    public XstsAuthService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public XstsAuthService() {
        this(HttpClient.newHttpClient());
    }

    public XstsToken authorize(XboxToken xboxToken) {
        if (xboxToken == null)
            throw new AuthException("Xbox token is required for XSTS authorization.");

        if (isBlank(xboxToken.token()))
            throw new AuthException("Xbox token value is required for XSTS authorization.");

        HttpRequest request = new XstsAuthorizeRequestBuilder(xboxToken.token()).build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleAuthorizeResponse(response);
        } catch (AuthException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthException("Interrupted while authorizing with XSTS.", exception);
        } catch (IOException exception) {
            throw new AuthException("Network error while authorizing with XSTS.", exception);
        } catch (Exception exception) {
            throw new AuthException("Failed to authorize with XSTS.", exception);
        }
    }

    private XstsToken handleAuthorizeResponse(HttpResponse<String> response) {
        XstsAuthorizeResponse authorizeResponse =
                XstsAuthorizeResponseParser.parse(response.body());

        if (authorizeResponse.isError())
            throw new AuthException(buildXstsErrorMessage(response.statusCode(), authorizeResponse.error()));

        if (!authorizeResponse.isSuccess())
            throw new AuthException(
                    "XSTS authorize endpoint returned an unrecognized response. HTTP status: " + response.statusCode()
            );

        if (response.statusCode() != 200)
            throw new AuthException(
                    "XSTS authorize endpoint returned unexpected HTTP status " + response.statusCode()
            );

        XstsToken token = authorizeResponse.token();
        validateToken(token);
        return token;
    }

    private void validateToken(XstsToken token) {
        if (token == null)
            throw new AuthException("XSTS token response did not contain a token.");

        if (isBlank(token.token()))
            throw new AuthException("XSTS token response did not contain a token value.");

        if (isBlank(token.uhs()))
            throw new AuthException("XSTS token response did not contain a user hash.");
    }

    private String buildXstsErrorMessage(int statusCode, XstsError error) {
        if (error != null) {
            String userMessage = XboxAuthErrorMessages.describe(error.xErr(), error.redirect());
            if (!isBlank(userMessage)) {
                return userMessage;
            }
        }

        var builder = new StringBuilder()
                .append("XSTS authorize endpoint returned an error")
                .append(" (HTTP ").append(statusCode).append(")");

        if (error != null) {
            if (!isBlank(error.message())) {
                builder.append(": ").append(error.message());
            }

            if (error.xErr() != null) {
                builder.append(" [xerr=").append(formatXErr(error.xErr())).append("]");
            }

            if (!isBlank(error.redirect())) {
                builder.append(" [redirect=").append(error.redirect()).append("]");
            }

            if (!isBlank(error.identity())) {
                builder.append(" [identity=").append(error.identity()).append("]");
            }
        }

        return builder.toString();
    }

    private String formatXErr(long xErr) {
        return String.format(Locale.ROOT, "0x%08X", xErr);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
