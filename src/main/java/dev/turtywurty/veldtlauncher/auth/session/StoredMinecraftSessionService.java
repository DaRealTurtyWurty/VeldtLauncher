package dev.turtywurty.veldtlauncher.auth.session;

import dev.turtywurty.veldtlauncher.auth.AuthConfig;
import dev.turtywurty.veldtlauncher.auth.AuthException;
import dev.turtywurty.veldtlauncher.auth.microsoft.MicrosoftTokenSet;
import dev.turtywurty.veldtlauncher.auth.minecraft.MinecraftAccessToken;
import dev.turtywurty.veldtlauncher.auth.minecraft.MinecraftAuthService;
import dev.turtywurty.veldtlauncher.auth.minecraft.MinecraftAuthenticationService;
import dev.turtywurty.veldtlauncher.auth.minecraft.MinecraftProfile;
import dev.turtywurty.veldtlauncher.auth.minecraft.MinecraftProfileLookupService;
import dev.turtywurty.veldtlauncher.auth.minecraft.MinecraftProfileService;
import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftOAuthClient;
import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftOAuthService;
import dev.turtywurty.veldtlauncher.auth.session.secret.OSCredentialSecretStore;
import dev.turtywurty.veldtlauncher.auth.session.secret.SecretStore;
import dev.turtywurty.veldtlauncher.auth.xbox.XboxAuthService;
import dev.turtywurty.veldtlauncher.auth.xbox.XboxAuthenticationService;
import dev.turtywurty.veldtlauncher.auth.xbox.XboxToken;
import dev.turtywurty.veldtlauncher.auth.xbox.xsts.XstsAuthService;
import dev.turtywurty.veldtlauncher.auth.xbox.xsts.XstsAuthorizationService;
import dev.turtywurty.veldtlauncher.auth.xbox.xsts.XstsToken;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public final class StoredMinecraftSessionService {
    public static final StoredMinecraftSessionService INSTANCE = new StoredMinecraftSessionService(
            JsonSessionStore.INSTANCE,
            OSCredentialSecretStore.INSTANCE,
            new MicrosoftOAuthClient(),
            new XboxAuthService(),
            new XstsAuthService(),
            new MinecraftAuthService(),
            new MinecraftProfileService()
    );

    private final SessionStore sessionStore;
    private final SecretStore secretStore;
    private final MicrosoftOAuthService microsoftOAuthService;
    private final XboxAuthenticationService xboxAuthenticationService;
    private final XstsAuthorizationService xstsAuthorizationService;
    private final MinecraftAuthenticationService minecraftAuthenticationService;
    private final MinecraftProfileLookupService minecraftProfileLookupService;

    public StoredMinecraftSessionService(
            SessionStore sessionStore,
            SecretStore secretStore,
            MicrosoftOAuthService microsoftOAuthService,
            XboxAuthenticationService xboxAuthenticationService,
            XstsAuthorizationService xstsAuthorizationService,
            MinecraftAuthenticationService minecraftAuthenticationService,
            MinecraftProfileLookupService minecraftProfileLookupService
    ) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore");
        this.microsoftOAuthService = Objects.requireNonNull(microsoftOAuthService, "microsoftOAuthService");
        this.xboxAuthenticationService = Objects.requireNonNull(xboxAuthenticationService, "xboxAuthenticationService");
        this.xstsAuthorizationService = Objects.requireNonNull(xstsAuthorizationService, "xstsAuthorizationService");
        this.minecraftAuthenticationService = Objects.requireNonNull(minecraftAuthenticationService, "minecraftAuthenticationService");
        this.minecraftProfileLookupService = Objects.requireNonNull(minecraftProfileLookupService, "minecraftProfileLookupService");
    }

    public Optional<MinecraftSession> load() {
        return this.sessionStore.load().map(this::refreshSession);
    }

    public Optional<MinecraftSession> load(String userId) {
        return this.sessionStore.load(userId).map(this::refreshSession);
    }

    public MinecraftSession require() {
        return load().orElseThrow(() -> new AuthException("No stored Minecraft session is available."));
    }

    public MinecraftSession require(String userId) {
        return load(userId).orElseThrow(() -> new AuthException("No stored Minecraft session exists for user: " + userId));
    }

    private MinecraftSession refreshSession(StoredSessionMetadata storedSession) {
        String userId = requireUserId(storedSession);
        String refreshToken = this.secretStore.load(refreshTokenKey(userId))
                .filter(token -> !token.isBlank())
                .orElseThrow(() -> new AuthException("No Microsoft refresh token is stored for user: " + userId));

        MicrosoftTokenSet microsoftToken = this.microsoftOAuthService.refreshAccessToken(AuthConfig.getClientId(), refreshToken);
        XboxToken xboxToken = this.xboxAuthenticationService.authenticate(microsoftToken.accessToken());
        XstsToken xstsToken = this.xstsAuthorizationService.authorize(xboxToken);
        MinecraftAccessToken minecraftToken = this.minecraftAuthenticationService.authenticate(xstsToken);
        MinecraftProfile profile = this.minecraftProfileLookupService.lookupProfile(minecraftToken);

        String nextRefreshToken = microsoftToken.refreshToken() == null || microsoftToken.refreshToken().isBlank()
                ? refreshToken
                : microsoftToken.refreshToken();
        long now = System.currentTimeMillis();
        String profileId = profile.id() == null || profile.id().isBlank() ? userId : profile.id();
        String accountId = minecraftToken.username() == null || minecraftToken.username().isBlank()
                ? storedSession.accountId()
                : minecraftToken.username();
        String skinUrl = resolveSkinUrl(profile);

        this.sessionStore.save(new StoredSessionMetadata(
                profileId,
                profile.name(),
                now + (minecraftToken.expiresIn() * 1000L),
                accountId,
                now,
                skinUrl
        ));
        this.sessionStore.setLastSession(profileId);
        this.secretStore.save(refreshTokenKey(profileId), nextRefreshToken);
        this.secretStore.save(minecraftAccessTokenKey(profileId), minecraftToken.accessToken());

        return new MinecraftSession(
                profile,
                minecraftToken.accessToken(),
                nextRefreshToken,
                accountId,
                AuthConfig.getClientId()
        );
    }

    private String requireUserId(StoredSessionMetadata storedSession) {
        if (storedSession == null || storedSession.userId() == null || storedSession.userId().isBlank())
            throw new AuthException("Stored session is missing a user id.");

        return storedSession.userId();
    }

    private String resolveSkinUrl(MinecraftProfile profile) {
        if (profile == null || profile.skins() == null)
            return null;

        return Arrays.stream(profile.skins())
                .filter(Objects::nonNull)
                .map(MinecraftProfile.Skin::url)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String refreshTokenKey(String userId) {
        return "profile:" + userId + ":microsoft_refresh_token";
    }

    private String minecraftAccessTokenKey(String userId) {
        return "profile:" + userId + ":minecraft_access_token";
    }
}
