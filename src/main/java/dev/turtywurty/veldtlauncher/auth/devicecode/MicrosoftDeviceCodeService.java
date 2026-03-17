package dev.turtywurty.veldtlauncher.auth.devicecode;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;
import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftTokenSet;

import java.util.List;
import java.util.function.Consumer;

public interface MicrosoftDeviceCodeService {
    MicrosoftDeviceCode requestDeviceCode(String clientId);

    MicrosoftDeviceCode requestDeviceCode(String clientId, List<String> scopes);

    default MicrosoftTokenSet awaitToken(String clientId, MicrosoftDeviceCode deviceCode) {
        return awaitToken(clientId, deviceCode, null);
    }

    MicrosoftTokenSet awaitToken(String clientId, MicrosoftDeviceCode deviceCode, Consumer<AuthEvent> eventConsumer);
}
