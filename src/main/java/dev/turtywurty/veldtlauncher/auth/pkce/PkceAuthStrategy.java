package dev.turtywurty.veldtlauncher.auth.pkce;

import dev.turtywurty.veldtlauncher.auth.*;
import dev.turtywurty.veldtlauncher.auth.pkce.event.*;
import dev.turtywurty.veldtlauncher.auth.pkce.http.AuthCallbackResult;
import dev.turtywurty.veldtlauncher.auth.pkce.http.AuthorizationRequestBuilder;
import dev.turtywurty.veldtlauncher.auth.pkce.http.CallbackServer;
import dev.turtywurty.veldtlauncher.auth.pkce.http.LocalCallbackServer;
import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftOAuthClient;
import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftOAuthService;
import dev.turtywurty.veldtlauncher.auth.pkce.microsoft.MicrosoftTokenSet;
import dev.turtywurty.veldtlauncher.auth.pkce.minecraft.*;
import dev.turtywurty.veldtlauncher.auth.pkce.xbox.XboxAuthService;
import dev.turtywurty.veldtlauncher.auth.pkce.xbox.XboxAuthenticationService;
import dev.turtywurty.veldtlauncher.auth.pkce.xbox.XboxToken;
import dev.turtywurty.veldtlauncher.auth.pkce.xbox.xsts.XstsAuthService;
import dev.turtywurty.veldtlauncher.auth.pkce.xbox.xsts.XstsAuthorizationService;
import dev.turtywurty.veldtlauncher.auth.pkce.xbox.xsts.XstsToken;
import dev.turtywurty.veldtlauncher.event.EventStream;

import java.net.URI;
import java.util.Objects;
import java.util.concurrent.*;

public class PkceAuthStrategy implements AuthStrategy {
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
                new MinecraftProfileService()
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
            MinecraftProfileLookupService minecraftProfileLookupService
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
            try {
                openBrowser(authUri);
            } catch (AuthException exception) {
                callbackServer.stop();
                throw exception;
            }

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
            MinecraftProfile profile = minecraftProfileLookupService.getMinecraftProfile(minecraftToken);
            emit(new MinecraftProfileFetchedEvent(profile.name(), profile.id()));
            emit(new AuthenticationSucceededEvent(profile.name(), profile.id()));

            return new MinecraftSession(profile, minecraftToken.accessToken(), microsoftToken.refreshToken());
        } catch (RuntimeException exception) {
            emit(new AuthenticationFailedEvent(errorMessage(exception)));
            throw exception;
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
        } finally {
            callbackServer.stop();
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
