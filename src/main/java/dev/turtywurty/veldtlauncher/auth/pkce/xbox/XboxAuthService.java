package dev.turtywurty.veldtlauncher.auth.pkce.xbox;

import dev.turtywurty.veldtlauncher.auth.AuthException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;

public class XboxAuthService implements XboxAuthenticationService {
    private final HttpClient httpClient;

    public XboxAuthService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public XboxAuthService() {
        this(HttpClient.newHttpClient());
    }

    public XboxToken authenticate(String accessToken) {
        HttpRequest request = new XboxAuthenticationRequestBuilder(accessToken).build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleAuthenticationResponse(response);
        } catch (AuthException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthException("Interrupted while authenticating with Xbox Live.", exception);
        } catch (IOException exception) {
            throw new AuthException("Network error while authenticating with Xbox Live.", exception);
        } catch (Exception exception) {
            throw new AuthException("Failed to authenticate with Xbox Live.", exception);
        }
    }

    private XboxToken handleAuthenticationResponse(HttpResponse<String> response) {
        XboxAuthenticationResponse authenticationResponse =
                XboxAuthenticationResponseParser.parse(response.body());

        if (authenticationResponse.isError())
            throw new AuthException(buildXboxErrorMessage(response.statusCode(), authenticationResponse.error()));

        if (!authenticationResponse.isSuccess())
            throw new AuthException(
                    "Xbox user auth endpoint returned an unrecognized response. HTTP status: " + response.statusCode()
            );

        if (response.statusCode() != 200)
            throw new AuthException(
                    "Xbox user auth endpoint returned unexpected HTTP status " + response.statusCode()
            );

        XboxToken token = authenticationResponse.token();
        validateToken(token);
        return token;
    }

    private void validateToken(XboxToken token) {
        if (token == null)
            throw new AuthException("Xbox token response did not contain a token.");

        if (isBlank(token.token()))
            throw new AuthException("Xbox token response did not contain a token value.");

        if (isBlank(token.uhs()))
            throw new AuthException("Xbox token response did not contain a user hash.");
    }

    private String buildXboxErrorMessage(int statusCode, XboxError error) {
        if (error != null) {
            String userMessage = XboxAuthErrorMessages.describe(error.xErr(), error.redirect());
            if (!isBlank(userMessage)) {
                return userMessage;
            }
        }

        var builder = new StringBuilder()
                .append("Xbox user auth endpoint returned an error")
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
