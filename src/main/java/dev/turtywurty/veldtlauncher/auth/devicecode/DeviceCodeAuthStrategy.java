package dev.turtywurty.veldtlauncher.auth.devicecode;

import dev.turtywurty.veldtlauncher.auth.AuthConfig;
import dev.turtywurty.veldtlauncher.auth.AuthEvent;
import dev.turtywurty.veldtlauncher.auth.AuthException;
import dev.turtywurty.veldtlauncher.auth.AuthStrategy;
import dev.turtywurty.veldtlauncher.auth.devicecode.event.DeviceCodePollingStartedEvent;
import dev.turtywurty.veldtlauncher.auth.devicecode.event.DeviceCodeRequestedEvent;
import dev.turtywurty.veldtlauncher.auth.devicecode.event.DeviceCodeSucceededEvent;
import dev.turtywurty.veldtlauncher.auth.browser.BrowserOpener;
import dev.turtywurty.veldtlauncher.auth.browser.DesktopBrowserOpener;
import dev.turtywurty.veldtlauncher.auth.event.*;
import dev.turtywurty.veldtlauncher.auth.microsoft.MicrosoftTokenSet;
import dev.turtywurty.veldtlauncher.auth.minecraft.*;
import dev.turtywurty.veldtlauncher.auth.session.secret.OSCredentialSecretStore;
import dev.turtywurty.veldtlauncher.auth.session.secret.SecretStore;
import dev.turtywurty.veldtlauncher.auth.xbox.XboxAuthService;
import dev.turtywurty.veldtlauncher.auth.xbox.XboxAuthenticationService;
import dev.turtywurty.veldtlauncher.auth.xbox.XboxToken;
import dev.turtywurty.veldtlauncher.auth.xbox.xsts.XstsAuthService;
import dev.turtywurty.veldtlauncher.auth.xbox.xsts.XstsAuthorizationService;
import dev.turtywurty.veldtlauncher.auth.xbox.xsts.XstsToken;
import dev.turtywurty.veldtlauncher.auth.session.*;
import dev.turtywurty.veldtlauncher.event.EventStream;

import java.net.URI;
import java.util.Objects;

public class DeviceCodeAuthStrategy implements AuthStrategy {
    private final EventStream eventStream;
    private final BrowserOpener browserOpener;
    private final MicrosoftDeviceCodeService microsoftDeviceCodeService;
    private final XboxAuthenticationService xboxAuthenticationService;
    private final XstsAuthorizationService xstsAuthorizationService;
    private final MinecraftAuthenticationService minecraftAuthenticationService;
    private final MinecraftProfileLookupService minecraftProfileLookupService;
    private final SessionStore sessionStore;
    private final SecretStore secretStore;

    public DeviceCodeAuthStrategy(EventStream eventStream) {
        this(
                eventStream,
                new DesktopBrowserOpener(),
                new MicrosoftDeviceCodeClient(),
                new XboxAuthService(),
                new XstsAuthService(),
                new MinecraftAuthService(),
                new MinecraftProfileService(),
                JsonSessionStore.INSTANCE,
                OSCredentialSecretStore.INSTANCE
        );
    }

    public DeviceCodeAuthStrategy(
            EventStream eventStream,
            BrowserOpener browserOpener,
            MicrosoftDeviceCodeService microsoftDeviceCodeService,
            XboxAuthenticationService xboxAuthenticationService,
            XstsAuthorizationService xstsAuthorizationService,
            MinecraftAuthenticationService minecraftAuthenticationService,
            MinecraftProfileLookupService minecraftProfileLookupService,
            SessionStore sessionStore,
            SecretStore secretStore
    ) {
        this.eventStream = eventStream;
        this.browserOpener = Objects.requireNonNull(browserOpener, "browserOpener");
        this.microsoftDeviceCodeService = Objects.requireNonNull(microsoftDeviceCodeService, "microsoftDeviceCodeService");
        this.xboxAuthenticationService = Objects.requireNonNull(xboxAuthenticationService, "xboxAuthenticationService");
        this.xstsAuthorizationService = Objects.requireNonNull(xstsAuthorizationService, "xstsAuthorizationService");
        this.minecraftAuthenticationService = Objects.requireNonNull(minecraftAuthenticationService, "minecraftAuthenticationService");
        this.minecraftProfileLookupService = Objects.requireNonNull(minecraftProfileLookupService, "minecraftProfileLookupService");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
        this.secretStore = Objects.requireNonNull(secretStore, "secretStore");
    }

    @Override
    public MinecraftSession authenticate() throws AuthException {
        emit(new AuthenticationStartedEvent());
        try {
            MicrosoftDeviceCode deviceCode = microsoftDeviceCodeService.requestDeviceCode(AuthConfig.getClientId());
            emit(new DeviceCodeRequestedEvent(
                    deviceCode.userCode(),
                    URI.create(deviceCode.verificationUri()),
                    deviceCode.verificationUriComplete() != null ? URI.create(deviceCode.verificationUriComplete()) : null,
                    deviceCode.expiresIn(),
                    deviceCode.interval(),
                    deviceCode.message()
            ));
            openBrowser(URI.create(deviceCode.verificationUri()));
            emit(new DeviceCodePollingStartedEvent(deviceCode.userCode(), deviceCode.interval(), deviceCode.expiresIn()));
            MicrosoftTokenSet microsoftToken = microsoftDeviceCodeService.awaitToken(
                    AuthConfig.getClientId(),
                    deviceCode,
                    this::emit
            );
            emit(new DeviceCodeSucceededEvent(
                    microsoftToken.expiresIn(),
                    microsoftToken.tokenType(),
                    microsoftToken.scope()
            ));
            emit(new MicrosoftLoginSucceededEvent());

            emit(new XboxAuthStartedEvent());
            XboxToken xboxToken = xboxAuthenticationService.authenticate(microsoftToken.accessToken());
            XstsToken xstsToken = xstsAuthorizationService.authorize(xboxToken);
            MinecraftAccessToken minecraftToken = minecraftAuthenticationService.authenticate(xstsToken);
            MinecraftProfile profile = minecraftProfileLookupService.lookupProfile(minecraftToken);
            emit(new MinecraftProfileFetchedEvent(profile.name(), profile.id()));
            emit(new AuthenticationSucceededEvent(profile.name(), profile.id()));

            this.sessionStore.save(new StoredSessionMetadata(profile.id(), profile.name(), System.currentTimeMillis() + (minecraftToken.expiresIn() * 1000L), minecraftToken.username()));
            this.sessionStore.setLastSession(profile.id());

            this.secretStore.save("profile:" + profile.id() + ":microsoft_refresh_token", microsoftToken.refreshToken());
            this.secretStore.save("profile:" + profile.id() + ":minecraft_access_token", minecraftToken.accessToken());
            this.secretStore.save("profile:" + profile.id() + ":minecraft_access_token", minecraftToken.accessToken());

            return new MinecraftSession(profile, minecraftToken.accessToken(), microsoftToken.refreshToken());
        } catch (RuntimeException exception) {
            emit(new AuthenticationFailedEvent(errorMessage(exception)));
            throw exception;
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String displayName() {
        return "Device Code";
    }

    @Override
    public EventStream eventStream() {
        return this.eventStream;
    }

    private void openBrowser(URI uri) {
        emit(new OpeningBrowserEvent());
        browserOpener.open(uri);
    }

    private void emit(AuthEvent event) {
        if (event != null && this.eventStream != null) {
            this.eventStream.emit(event);
        }
    }

    private String errorMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank())
            return "Authentication failed.";

        return throwable.getMessage();
    }
}
