package dev.turtywurty.veldtlauncher.auth.pkce.xbox;

public interface XboxAuthenticationService {
    XboxToken authenticate(String accessToken);
}
