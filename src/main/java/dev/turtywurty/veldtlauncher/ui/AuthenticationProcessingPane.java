package dev.turtywurty.veldtlauncher.ui;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;
import dev.turtywurty.veldtlauncher.auth.pkce.event.AuthenticationFailedEvent;
import dev.turtywurty.veldtlauncher.auth.pkce.event.AuthenticationStartedEvent;
import dev.turtywurty.veldtlauncher.auth.pkce.event.AuthenticationSucceededEvent;
import dev.turtywurty.veldtlauncher.auth.pkce.event.AuthorizationCallbackReceivedEvent;
import dev.turtywurty.veldtlauncher.auth.pkce.event.CallbackServerStartedEvent;
import dev.turtywurty.veldtlauncher.auth.pkce.event.MicrosoftLoginSucceededEvent;
import dev.turtywurty.veldtlauncher.auth.pkce.event.MinecraftProfileFetchedEvent;
import dev.turtywurty.veldtlauncher.auth.pkce.event.OpeningBrowserEvent;
import dev.turtywurty.veldtlauncher.auth.pkce.event.WaitingForCallbackEvent;
import dev.turtywurty.veldtlauncher.auth.pkce.event.XboxAuthStartedEvent;
import dev.turtywurty.veldtlauncher.event.EventListener;
import dev.turtywurty.veldtlauncher.event.EventStream;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class AuthenticationProcessingPane extends AnchorPane {
    public AuthenticationProcessingPane(EventStream eventStream, Runnable onBack) {
        getStylesheets().add(Objects.requireNonNull(
                AuthenticationProcessingPane.class.getResource("authenticate-processing-pane.css"),
                "Missing stylesheet: authenticate-processing-pane.css"
        ).toExternalForm());

        getStyleClass().add("authentication-processing-pane");

        var content = new VBox();
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(48));
        content.getStyleClass().add("authenticate-processing-content");
        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);

        var card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setFillWidth(true);
        card.setMaxWidth(420);
        card.getStyleClass().add("authentication-processing-card");

        var indicator = new ProgressIndicator();
        indicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        indicator.getStyleClass().add("authentication-progress-indicator");

        var title = new Text("Authenticating...");
        title.getStyleClass().add("authentication-processing-title");

        var status = new Text("Starting Microsoft sign-in.");
        status.getStyleClass().add("authentication-processing-status");
        status.wrappingWidthProperty().bind(card.maxWidthProperty().subtract(56));

        var details = new Text("Your browser will open and this launcher will keep listening for the callback.");
        details.getStyleClass().add("authentication-processing-details");
        details.wrappingWidthProperty().bind(card.maxWidthProperty().subtract(56));

        var backButton = new Button("Back");
        backButton.getStyleClass().add("authentication-processing-back-button");
        backButton.setManaged(false);
        backButton.setVisible(false);
        backButton.setOnAction(_ -> {
            if (onBack != null) {
                onBack.run();
            }
        });

        card.getChildren().addAll(indicator, title, status, details, backButton);
        content.getChildren().add(card);
        getChildren().add(content);

        AtomicReference<EventListener<AuthEvent>> listenerRef = new AtomicReference<>();
        sceneProperty().addListener((_, oldScene, newScene) -> {
            EventListener<AuthEvent> currentListener = listenerRef.getAndSet(null);
            if (currentListener != null) {
                eventStream.unregisterListener(currentListener);
            }

            if (newScene == null)
                return;

            EventListener<AuthEvent> listener = eventStream.registerListener(AuthEvent.class, event ->
                    Platform.runLater(() -> applyEvent(event, indicator, title, status, details, backButton)));
            listenerRef.set(listener);
        });
    }

    private void applyEvent(
            AuthEvent event,
            ProgressIndicator indicator,
            Text title,
            Text status,
            Text details,
            Button backButton
    ) {
        switch (event) {
            case AuthenticationStartedEvent ignored -> {
                title.setText("Authenticating...");
                status.setText("Preparing your secure Microsoft login.");
                details.setText("The launcher is setting up a local callback so it can finish sign-in.");
            }
            case CallbackServerStartedEvent callbackEvent -> {
                status.setText("Callback server is ready.");
                details.setText("Listening on " + callbackEvent.redirectUri() + " for the Microsoft redirect.");
            }
            case OpeningBrowserEvent ignored -> {
                status.setText("Opening browser...");
                details.setText("Approve the sign-in request in your browser to continue.");
            }
            case WaitingForCallbackEvent ignored -> {
                status.setText("Waiting for Microsoft callback...");
                details.setText("Finish sign-in in your browser, then return here while the launcher receives the response.");
            }
            case AuthorizationCallbackReceivedEvent callbackEvent -> {
                status.setText(callbackEvent.error()
                        ? "Microsoft returned an error."
                        : "Authorization callback received.");
                details.setText(callbackEvent.error()
                        ? "The launcher received an error response from the Microsoft sign-in flow."
                        : "Continuing with Xbox and Minecraft authentication.");
            }
            case MicrosoftLoginSucceededEvent ignored -> {
                status.setText("Microsoft login complete.");
                details.setText("Requesting Xbox Live credentials for your Microsoft account.");
            }
            case XboxAuthStartedEvent ignored -> {
                status.setText("Authorizing with Xbox Live...");
                details.setText("Exchanging your Microsoft session for Xbox and XSTS tokens.");
            }
            case MinecraftProfileFetchedEvent profileEvent -> {
                title.setText("Minecraft profile found.");
                status.setText("Signed in as " + profileEvent.username() + ".");
                details.setText("Profile " + profileEvent.uuid() + " was fetched successfully.");
            }
            case AuthenticationSucceededEvent succeededEvent -> {
                indicator.setProgress(1);
                title.setText("Authentication complete.");
                status.setText("Welcome, " + succeededEvent.username() + ".");
                details.setText("Your Minecraft session is ready.");
                backButton.setManaged(false);
                backButton.setVisible(false);
            }
            case AuthenticationFailedEvent failedEvent -> {
                if (!indicator.getStyleClass().contains("authentication-progress-indicator-failed")) {
                    indicator.getStyleClass().add("authentication-progress-indicator-failed");
                }

                title.setText("Authentication failed.");
                status.setText(failedEvent.message());
                details.setText("Try again after fixing the underlying issue.");
                backButton.setManaged(true);
                backButton.setVisible(true);
            }
            default -> {
            }
        }
    }
}
