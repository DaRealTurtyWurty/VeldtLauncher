package dev.turtywurty.veldtlauncher.auth.devicecode;

import dev.turtywurty.veldtlauncher.auth.AuthException;
import dev.turtywurty.veldtlauncher.auth.AuthEvent;
import dev.turtywurty.veldtlauncher.auth.devicecode.event.DeviceCodeAuthorizationDeclinedEvent;
import dev.turtywurty.veldtlauncher.auth.devicecode.event.DeviceCodeAuthorizationPendingEvent;
import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftError;
import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftTokenResponse;
import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftTokenResponseParser;
import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftTokenSet;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MicrosoftDeviceCodeClient implements MicrosoftDeviceCodeService {
    private static final List<String> DEFAULT_SCOPES = List.of("XboxLive.signin", "offline_access");

    private final HttpClient httpClient;

    public MicrosoftDeviceCodeClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public MicrosoftDeviceCodeClient() {
        this(HttpClient.newHttpClient());
    }

    @Override
    public MicrosoftDeviceCode requestDeviceCode(String clientId) {
        return requestDeviceCode(clientId, DEFAULT_SCOPES);
    }

    @Override
    public MicrosoftDeviceCode requestDeviceCode(String clientId, List<String> scopes) {
        HttpRequest request = new MicrosoftDeviceCodeRequestBuilder(clientId, scopes).build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleDeviceCodeResponse(response);
        } catch (AuthException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthException("Interrupted while requesting a Microsoft device code.", exception);
        } catch (IOException exception) {
            throw new AuthException("Network error while requesting a Microsoft device code.", exception);
        } catch (Exception exception) {
            throw new AuthException("Failed to request a Microsoft device code.", exception);
        }
    }

    @Override
    public MicrosoftTokenSet awaitToken(
            String clientId,
            MicrosoftDeviceCode deviceCode,
            Consumer<AuthEvent> eventConsumer
    ) {
        validateDeviceCode(deviceCode);

        long intervalSeconds = Math.max(1, deviceCode.interval());
        long expiresInSeconds = Math.max(intervalSeconds, deviceCode.expiresIn());
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(expiresInSeconds);

        while (System.nanoTime() < deadline) {
            HttpRequest request = new MicrosoftDeviceCodeTokenRequestBuilder(clientId, deviceCode.deviceCode()).build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                MicrosoftTokenResponse tokenResponse = MicrosoftTokenResponseParser.parse(response.body());

                if (tokenResponse.isSuccess()) {
                    return handleTokenSuccess(response.statusCode(), tokenResponse.tokenSet());
                }

                MicrosoftError error = tokenResponse.error();
                if (error == null) {
                    throw new AuthException(
                            "Microsoft token endpoint returned an unrecognized device-code response. HTTP status: "
                                    + response.statusCode()
                    );
                }

                switch (error.getType()) {
                    case AUTHORIZATION_PENDING -> {
                        emit(eventConsumer, new DeviceCodeAuthorizationPendingEvent(intervalSeconds));
                        sleep(intervalSeconds);
                    }
                    case SLOW_DOWN -> {
                        intervalSeconds += 5;
                        emit(eventConsumer, new DeviceCodeAuthorizationPendingEvent(intervalSeconds));
                        sleep(intervalSeconds);
                    }
                    case AUTHORIZATION_DECLINED -> {
                        emit(eventConsumer, new DeviceCodeAuthorizationDeclinedEvent());
                        throw new AuthException("Microsoft device-code authorization was declined by the user.");
                    }
                    case EXPIRED_TOKEN ->
                            throw new AuthException("Microsoft device code expired before authorization completed.");
                    case BAD_VERIFICATION_CODE ->
                            throw new AuthException("Microsoft device-code token polling used an invalid device code.");
                    default -> throw new AuthException(buildOAuthErrorMessage(response.statusCode(), error));
                }
            } catch (AuthException exception) {
                throw exception;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AuthException("Interrupted while waiting for Microsoft device-code authorization.", exception);
            } catch (IOException exception) {
                throw new AuthException("Network error while polling Microsoft device-code authorization.", exception);
            } catch (Exception exception) {
                throw new AuthException("Failed while polling Microsoft device-code authorization.", exception);
            }
        }

        throw new AuthException("Timed out while waiting for Microsoft device-code authorization.");
    }

    private MicrosoftDeviceCode handleDeviceCodeResponse(HttpResponse<String> response) {
        MicrosoftDeviceCodeResponse deviceCodeResponse = MicrosoftDeviceCodeResponseParser.parse(response.body());

        if (deviceCodeResponse.isError())
            throw new AuthException(buildOAuthErrorMessage(response.statusCode(), deviceCodeResponse.error()));

        if (!deviceCodeResponse.isSuccess())
            throw new AuthException(
                    "Microsoft device code endpoint returned an unrecognized response. HTTP status: "
                            + response.statusCode()
            );

        if (response.statusCode() != 200)
            throw new AuthException(
                    "Microsoft device code endpoint returned unexpected HTTP status " + response.statusCode()
            );

        MicrosoftDeviceCode deviceCode = deviceCodeResponse.deviceCode();
        validateDeviceCode(deviceCode);
        return deviceCode;
    }

    private MicrosoftTokenSet handleTokenSuccess(int statusCode, MicrosoftTokenSet tokenSet) {
        if (statusCode != 200)
            throw new AuthException("Microsoft token endpoint returned unexpected HTTP status " + statusCode);

        validateTokenSet(tokenSet);
        return tokenSet;
    }

    private void validateDeviceCode(MicrosoftDeviceCode deviceCode) {
        if (deviceCode == null)
            throw new AuthException("Microsoft device code response did not contain a device code.");

        if (isBlank(deviceCode.deviceCode()))
            throw new AuthException("Microsoft device code response did not contain a device_code.");

        if (isBlank(deviceCode.userCode()))
            throw new AuthException("Microsoft device code response did not contain a user_code.");

        if (isBlank(deviceCode.verificationUri()))
            throw new AuthException("Microsoft device code response did not contain a verification_uri.");

        if (deviceCode.expiresIn() <= 0)
            throw new AuthException(
                    "Microsoft device code response contained an invalid expires_in value: " + deviceCode.expiresIn()
            );

        if (deviceCode.interval() <= 0)
            throw new AuthException(
                    "Microsoft device code response contained an invalid interval value: " + deviceCode.interval()
            );
    }

    private void validateTokenSet(MicrosoftTokenSet tokenSet) {
        if (tokenSet == null)
            throw new AuthException("Token response did not contain a token set.");

        if (isBlank(tokenSet.accessToken()))
            throw new AuthException("Token response did not contain an access token.");

        if (isBlank(tokenSet.tokenType()))
            throw new AuthException("Token response did not contain a token type.");

        if (tokenSet.expiresIn() <= 0)
            throw new AuthException("Token response contained an invalid expires_in value: " + tokenSet.expiresIn());
    }

    private void sleep(long seconds) throws InterruptedException {
        TimeUnit.SECONDS.sleep(Math.max(1, seconds));
    }

    private String buildOAuthErrorMessage(int statusCode, MicrosoftError error) {
        StringBuilder builder = new StringBuilder()
                .append("Microsoft OAuth endpoint returned an error")
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

    private void emit(Consumer<AuthEvent> eventConsumer, AuthEvent event) {
        if (eventConsumer != null && event != null) {
            eventConsumer.accept(event);
        }
    }
}
