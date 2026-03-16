package dev.turtywurty.minecraftlauncher.auth.pkce.xbox;

public interface XboxAuthenticationService {
    XboxToken authenticate(String accessToken);
}
