package dev.turtywurty.veldtlauncher.auth.xbox;

public interface XboxAuthenticationService {
    XboxToken authenticate(String accessToken);
}
