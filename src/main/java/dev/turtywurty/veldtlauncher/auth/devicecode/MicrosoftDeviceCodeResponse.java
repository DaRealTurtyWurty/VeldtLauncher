package dev.turtywurty.veldtlauncher.auth.devicecode;

import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftError;

public record MicrosoftDeviceCodeResponse(
        MicrosoftDeviceCode deviceCode,
        MicrosoftError error
) {
    public static MicrosoftDeviceCodeResponse success(MicrosoftDeviceCode deviceCode) {
        return new MicrosoftDeviceCodeResponse(deviceCode, null);
    }

    public static MicrosoftDeviceCodeResponse error(MicrosoftError error) {
        return new MicrosoftDeviceCodeResponse(null, error);
    }

    public boolean isSuccess() {
        return deviceCode != null;
    }

    public boolean isError() {
        return error != null;
    }
}
