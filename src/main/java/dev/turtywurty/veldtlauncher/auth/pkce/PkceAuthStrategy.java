package dev.turtywurty.veldtlauncher.auth.pkce;

import dev.turtywurty.veldtlauncher.auth.AuthConfig;
import dev.turtywurty.veldtlauncher.auth.AuthEvent;
import dev.turtywurty.veldtlauncher.auth.AuthException;
import dev.turtywurty.veldtlauncher.auth.AuthStrategy;
import dev.turtywurty.veldtlauncher.auth.browser.BrowserOpener;
import dev.turtywurty.veldtlauncher.auth.browser.DesktopBrowserOpener;
import dev.turtywurty.veldtlauncher.auth.event.*;
import dev.turtywurty.veldtlauncher.auth.pkce.http.AuthCallbackResult;
import dev.turtywurty.veldtlauncher.auth.pkce.http.AuthorizationRequestBuilder;
import dev.turtywurty.veldtlauncher.auth.pkce.http.CallbackServer;
import dev.turtywurty.veldtlauncher.auth.pkce.http.LocalCallbackServer;
import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftOAuthClient;
import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftOAuthService;
import dev.turtywurty.veldtlauncher.auth.microsoft.MicrosoftTokenSet;
import dev.turtywurty.veldtlauncher.auth.minecraft.*;
import dev.turtywurty.veldtlauncher.auth.pkce.state.StateFactory;
import dev.turtywurty.veldtlauncher.auth.pkce.state.StateProvider;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;

public class PkceAuthStrategy implements AuthStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(PkceAuthStrategy.class);

    private final EventStream eventStream;
    private final PkceValuesProvider pkceValuesProvider;
    private final StateProvider stateProvider;
    private final CallbackServer callbackServer;
    private final BrowserOpener browserOpener;
    private final MicrosoftOAuthService microsoftOAuthService;
    private final XboxAuthenticationService xboxAuthenticationService;
    private final XstsAuthorizationService xstsAuthorizationService;
    private final MinecraftAuthenticationService minecraftAuthenticationService;
    private final MinecraftProfileLookupService minecraftProfileLookupService;
    private final SessionStore sessionStore;
    private final SecretStore secretStore;

    public PkceAuthStrategy(EventStream eventStream) {
        this(
                eventStream,
                new PkceValuesFactory(),
                new StateFactory(),
                new LocalCallbackServer(),
                new DesktopBrowserOpener(),
                new MicrosoftOAuthClient(),
                new XboxAuthService(),
                new XstsAuthService(),
                new MinecraftAuthService(),
                new MinecraftProfileService(),
                JsonSessionStore.INSTANCE,
                OSCredentialSecretStore.INSTANCE
        );
    }

    public PkceAuthStrategy(
            EventStream eventStream,
            PkceValuesProvider pkceValuesProvider,
            StateProvider stateProvider,
            CallbackServer callbackServer,
            BrowserOpener browserOpener,
            MicrosoftOAuthService microsoftOAuthService,
            XboxAuthenticationService xboxAuthenticationService,
            XstsAuthorizationService xstsAuthorizationService,
            MinecraftAuthenticationService minecraftAuthenticationService,
            MinecraftProfileLookupService minecraftProfileLookupService,
            SessionStore sessionStore,
            SecretStore secretStore
    ) {
        this.eventStream = eventStream;
        this.pkceValuesProvider = Objects.requireNonNull(pkceValuesProvider, "pkceValuesProvider");
        this.stateProvider = Objects.requireNonNull(stateProvider, "stateProvider");
        this.callbackServer = Objects.requireNonNull(callbackServer, "callbackServer");
        this.browserOpener = Objects.requireNonNull(browserOpener, "browserOpener");
        this.microsoftOAuthService = Objects.requireNonNull(microsoftOAuthService, "microsoftOAuthService");
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
            URI redirectUri = AuthConfig.getRedirectUri();
            String clientId = AuthConfig.getClientId();

            PkceValues pkceValues = pkceValuesProvider.create();
            String state = stateProvider.create();

            URI authUri = new AuthorizationRequestBuilder(clientId, redirectUri, pkceValues, state).build();

            CompletableFuture<AuthCallbackResult> callbackResultFuture = startCallbackServer();
            emit(new CallbackServerStartedEvent(redirectUri));
            openBrowser(authUri);

            emit(new WaitingForCallbackEvent());
            AuthCallbackResult callbackResult = waitForAuthorizationCallback(callbackResultFuture);
            emit(new AuthorizationCallbackReceivedEvent(callbackResult.isError()));
            if (!state.equals(callbackResult.state()))
                throw new AuthException("State mismatch in authorization callback.");

            if (callbackResult.isError())
                throw new AuthException("Error in authorization callback: " + (callbackResult.errorDescription() != null ? callbackResult.errorDescription() : "Unknown error"));

            MicrosoftTokenSet microsoftToken = microsoftOAuthService.exchangeAuthorizationCode(clientId, callbackResult.code(), String.valueOf(redirectUri), pkceValues.codeVerifier());
            emit(new MicrosoftLoginSucceededEvent());

            emit(new XboxAuthStartedEvent());
            XboxToken xboxToken = xboxAuthenticationService.authenticate(microsoftToken.accessToken());
            XstsToken xstsToken = xstsAuthorizationService.authorize(xboxToken);
            MinecraftAccessToken minecraftToken = minecraftAuthenticationService.authenticate(xstsToken);
            MinecraftProfile profile = minecraftProfileLookupService.lookupProfile(minecraftToken);
            emit(new MinecraftProfileFetchedEvent(profile.name(), profile.id()));
            emit(new AuthenticationSucceededEvent(profile.name(), profile.id()));

            String skinUrl = Arrays.stream(profile.skins())
                    .filter(Objects::nonNull)
                    .map(MinecraftProfile.Skin::url)
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst()
                    .orElse(null);

            long now = System.currentTimeMillis();
            this.sessionStore.save(new StoredSessionMetadata(
                    profile.id(),
                    profile.name(),
                    now + (minecraftToken.expiresIn() * 1000L),
                    minecraftToken.username(),
                    now,
                    skinUrl
            ));
            this.sessionStore.setLastSession(profile.id());

            this.secretStore.save("profile:" + profile.id() + ":microsoft_refresh_token", microsoftToken.refreshToken());
            this.secretStore.save("profile:" + profile.id() + ":minecraft_access_token", minecraftToken.accessToken());
            this.secretStore.save("profile:" + profile.id() + ":minecraft_access_token", minecraftToken.accessToken());

            return new MinecraftSession(
                    profile,
                    minecraftToken.accessToken(),
                    microsoftToken.refreshToken(),
                    minecraftToken.username(),
                    clientId
            );
        } catch (RuntimeException exception) {
            emit(new AuthenticationFailedEvent(errorMessage(exception)));
            throw exception;
        } finally {
            callbackServer.stop();
        }
    }

    private CompletableFuture<AuthCallbackResult> startCallbackServer() {
        CompletableFuture<AuthCallbackResult> futureResult = new CompletableFuture<>();
        callbackServer.start(futureResult::complete);
        return futureResult;
    }

    private AuthCallbackResult waitForAuthorizationCallback(CompletableFuture<AuthCallbackResult> futureResult) {
        try {
            return futureResult.get(10, TimeUnit.MINUTES);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthException("Thread was interrupted while waiting for authorization callback.", exception);
        } catch (TimeoutException exception) {
            throw new AuthException("Timed out while waiting for authorization callback.", exception);
        } catch (ExecutionException exception) {
            throw new AuthException("An error occurred while waiting for authorization callback.", exception.getCause());
        } catch (CancellationException exception) {
            throw new AuthException("Authorization callback was cancelled.", exception);
        }
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

        LOGGER.error("Authentication error", throwable);

        return throwable.getMessage();
    }

    @Override
    public boolean isAvailable() {
        return browserOpener.isSupported() && callbackServer.isSupported();
    }

    @Override
    public String displayName() {
        return "PKCE";
    }

    @Override
    public EventStream eventStream() {
        return this.eventStream;
    }
}
