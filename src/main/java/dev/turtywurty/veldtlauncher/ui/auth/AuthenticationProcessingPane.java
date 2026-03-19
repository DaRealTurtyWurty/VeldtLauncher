package dev.turtywurty.veldtlauncher.ui.auth;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;
import dev.turtywurty.veldtlauncher.auth.devicecode.event.DeviceCodeAuthorizationDeclinedEvent;
import dev.turtywurty.veldtlauncher.auth.devicecode.event.DeviceCodeAuthorizationPendingEvent;
import dev.turtywurty.veldtlauncher.auth.devicecode.event.DeviceCodePollingStartedEvent;
import dev.turtywurty.veldtlauncher.auth.devicecode.event.DeviceCodeRequestedEvent;
import dev.turtywurty.veldtlauncher.auth.devicecode.event.DeviceCodeSucceededEvent;
import dev.turtywurty.veldtlauncher.auth.event.AuthenticationFailedEvent;
import dev.turtywurty.veldtlauncher.auth.event.AuthenticationStartedEvent;
import dev.turtywurty.veldtlauncher.auth.event.AuthenticationSucceededEvent;
import dev.turtywurty.veldtlauncher.auth.event.AuthorizationCallbackReceivedEvent;
import dev.turtywurty.veldtlauncher.auth.event.CallbackServerStartedEvent;
import dev.turtywurty.veldtlauncher.auth.event.MicrosoftLoginSucceededEvent;
import dev.turtywurty.veldtlauncher.auth.event.MinecraftProfileFetchedEvent;
import dev.turtywurty.veldtlauncher.auth.event.OpeningBrowserEvent;
import dev.turtywurty.veldtlauncher.auth.event.WaitingForCallbackEvent;
import dev.turtywurty.veldtlauncher.auth.event.XboxAuthStartedEvent;
import dev.turtywurty.veldtlauncher.event.EventListener;
import dev.turtywurty.veldtlauncher.event.EventStream;
import dev.turtywurty.veldtlauncher.ui.Stylesheets;
import dev.turtywurty.veldtlauncher.ui.WindowChrome;
import dev.turtywurty.veldtlauncher.ui.dashboard.shell.DashboardShell;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicReference;

public class AuthenticationProcessingPane extends AnchorPane {
    public AuthenticationProcessingPane(EventStream eventStream, Runnable onBack) {
        Stylesheets.addAll(this, "authenticate-processing-pane.css", "shared-controls.css");

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

        var deviceCodeField = new TextField();
        deviceCodeField.setEditable(false);
        deviceCodeField.setFocusTraversable(false);
        deviceCodeField.setMaxWidth(Double.MAX_VALUE);
        deviceCodeField.getStyleClass().add("authentication-processing-copy-field");

        var deviceCodeCopyButton = new Button("Copy");
        deviceCodeCopyButton.getStyleClass().add("authentication-processing-copy-button");
        deviceCodeCopyButton.setContentDisplay(ContentDisplay.TEXT_ONLY);
        deviceCodeCopyButton.setOnAction(_ -> copyToClipboard(deviceCodeField.getText()));

        var deviceCodeRow = new HBox(10, deviceCodeField, deviceCodeCopyButton);
        deviceCodeRow.setAlignment(Pos.CENTER_LEFT);
        deviceCodeRow.setManaged(false);
        deviceCodeRow.setVisible(false);
        deviceCodeRow.getStyleClass().add("authentication-processing-copy-row");

        var verificationUriField = new TextField();
        verificationUriField.setEditable(false);
        verificationUriField.setFocusTraversable(false);
        verificationUriField.setMaxWidth(Double.MAX_VALUE);
        verificationUriField.getStyleClass().add("authentication-processing-copy-field");

        var verificationUriCopyButton = new Button("Copy");
        verificationUriCopyButton.getStyleClass().add("authentication-processing-copy-button");
        verificationUriCopyButton.setContentDisplay(ContentDisplay.TEXT_ONLY);
        verificationUriCopyButton.setOnAction(_ -> copyToClipboard(verificationUriField.getText()));

        var verificationUriRow = new HBox(10, verificationUriField, verificationUriCopyButton);
        verificationUriRow.setAlignment(Pos.CENTER_LEFT);
        verificationUriRow.setManaged(false);
        verificationUriRow.setVisible(false);
        verificationUriRow.getStyleClass().add("authentication-processing-copy-row");

        var backButton = new Button("Back");
        backButton.getStyleClass().add("authentication-processing-back-button");
        backButton.setManaged(false);
        backButton.setVisible(false);
        backButton.setOnAction(_ -> {
            if (onBack != null) {
                onBack.run();
            }
        });

        HBox.setHgrow(deviceCodeField, Priority.ALWAYS);
        HBox.setHgrow(verificationUriField, Priority.ALWAYS);

        card.getChildren().addAll(indicator, title, status, details, deviceCodeRow, verificationUriRow, backButton);
        content.getChildren().add(card);
        getChildren().add(content);

        var windowBar = new HBox(WindowChrome.createWindowControls(this));
        windowBar.setAlignment(Pos.CENTER_RIGHT);
        windowBar.setPadding(new Insets(12, 12, 0, 12));
        WindowChrome.installDragSupport(windowBar);
        AnchorPane.setTopAnchor(windowBar, 0.0);
        AnchorPane.setLeftAnchor(windowBar, 0.0);
        AnchorPane.setRightAnchor(windowBar, 0.0);
        getChildren().add(windowBar);

        AtomicReference<EventListener<AuthEvent>> listenerRef = new AtomicReference<>();
        AtomicReference<Timeline> countdownRef = new AtomicReference<>();
        AtomicReference<DeviceCodeRequestedEvent> deviceCodeRef = new AtomicReference<>();
        sceneProperty().addListener((_, _, newScene) -> {
            EventListener<AuthEvent> currentListener = listenerRef.getAndSet(null);
            if (currentListener != null) {
                eventStream.unregisterListener(currentListener);
            }

            if (newScene == null) {
                stopCountdown(countdownRef);
                return;
            }

            EventListener<AuthEvent> listener = eventStream.registerListener(AuthEvent.class, event ->
                    Platform.runLater(() -> applyEvent(
                            event,
                            indicator,
                            title,
                            status,
                            details,
                            deviceCodeRow,
                            deviceCodeField,
                            verificationUriRow,
                            verificationUriField,
                            backButton,
                            countdownRef,
                            deviceCodeRef
                    )));
            listenerRef.set(listener);
        });
    }

    private void applyEvent(
            AuthEvent event,
            ProgressIndicator indicator,
            Text title,
            Text status,
            Text details,
            HBox deviceCodeRow,
            TextField deviceCodeField,
            HBox verificationUriRow,
            TextField verificationUriField,
            Button backButton,
            AtomicReference<Timeline> countdownRef,
            AtomicReference<DeviceCodeRequestedEvent> deviceCodeRef
    ) {
        switch (event) {
            case AuthenticationStartedEvent ignored -> {
                stopCountdown(countdownRef);
                deviceCodeRef.set(null);
                hideCopyFields(deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                title.setText("Authenticating...");
                status.setText("Preparing your secure Microsoft login.");
                details.setText("The launcher is setting up a local callback so it can finish sign-in.");
            }
            case DeviceCodeRequestedEvent deviceCodeEvent -> {
                stopCountdown(countdownRef);
                deviceCodeRef.set(deviceCodeEvent);
                showCopyFields(deviceCodeEvent, deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                status.setText("Device code ready.");
                details.setText(buildDeviceCodeMessage(deviceCodeEvent));
            }
            case DeviceCodePollingStartedEvent _ -> {
                stopCountdown(countdownRef);
                showCopyFields(deviceCodeRef.get(), deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                status.setText("Waiting for device-code approval...");
                details.setText(buildDeviceCodePollingMessage(deviceCodeRef.get(), -1));
            }
            case DeviceCodeAuthorizationPendingEvent deviceCodeEvent -> {
                showCopyFields(deviceCodeRef.get(), deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                status.setText("Waiting for device-code approval...");
                startCountdown(deviceCodeEvent.pollingIntervalSeconds(), details, countdownRef, deviceCodeRef.get());
            }
            case DeviceCodeAuthorizationDeclinedEvent ignored -> {
                stopCountdown(countdownRef);
                showCopyFields(deviceCodeRef.get(), deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                status.setText("Device-code authorization declined.");
                details.setText("The Microsoft device-code request was declined before authentication could finish.");
            }
            case DeviceCodeSucceededEvent ignored -> {
                stopCountdown(countdownRef);
                hideCopyFields(deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                status.setText("Microsoft device-code login complete.");
                details.setText("Requesting Xbox Live credentials for your Microsoft account.");
            }
            case CallbackServerStartedEvent callbackEvent -> {
                stopCountdown(countdownRef);
                hideCopyFields(deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                status.setText("Callback server is ready.");
                details.setText("Listening on " + callbackEvent.redirectUri() + " for the Microsoft redirect.");
            }
            case OpeningBrowserEvent ignored -> {
                status.setText("Opening browser...");
                DeviceCodeRequestedEvent deviceCodeEvent = deviceCodeRef.get();
                if (deviceCodeEvent != null) {
                    showCopyFields(deviceCodeEvent, deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                } else {
                    hideCopyFields(deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                }
                details.setText(deviceCodeEvent != null
                        ? buildDeviceCodePollingMessage(deviceCodeEvent, -1)
                        : "Approve the sign-in request in your browser to continue.");
            }
            case WaitingForCallbackEvent ignored -> {
                stopCountdown(countdownRef);
                hideCopyFields(deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                status.setText("Waiting for Microsoft callback...");
                details.setText("Finish sign-in in your browser, then return here while the launcher receives the response.");
            }
            case AuthorizationCallbackReceivedEvent callbackEvent -> {
                stopCountdown(countdownRef);
                hideCopyFields(deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                status.setText(callbackEvent.error()
                        ? "Microsoft returned an error."
                        : "Authorization callback received.");
                details.setText(callbackEvent.error()
                        ? "The launcher received an error response from the Microsoft sign-in flow."
                        : "Continuing with Xbox and Minecraft authentication.");
            }
            case MicrosoftLoginSucceededEvent ignored -> {
                stopCountdown(countdownRef);
                hideCopyFields(deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                status.setText("Microsoft login complete.");
                details.setText("Requesting Xbox Live credentials for your Microsoft account.");
            }
            case XboxAuthStartedEvent ignored -> {
                stopCountdown(countdownRef);
                hideCopyFields(deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                status.setText("Authorizing with Xbox Live...");
                details.setText("Exchanging your Microsoft session for Xbox and XSTS tokens.");
            }
            case MinecraftProfileFetchedEvent profileEvent -> {
                stopCountdown(countdownRef);
                hideCopyFields(deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                title.setText("Minecraft profile found.");
                status.setText("Signed in as " + profileEvent.username() + ".");
                details.setText("Profile " + profileEvent.uuid() + " was fetched successfully.");
            }
            case AuthenticationSucceededEvent succeededEvent -> {
                stopCountdown(countdownRef);
                hideCopyFields(deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                indicator.setProgress(1);
                title.setText("Authentication complete.");
                status.setText("Welcome, " + succeededEvent.username() + ".");
                details.setText("Your Minecraft session is ready.");
                backButton.setManaged(false);
                backButton.setVisible(false);
                Scene scene = getScene();
                if (scene != null) {
                    DashboardShell.show(scene);
                }
            }
            case AuthenticationFailedEvent failedEvent -> {
                stopCountdown(countdownRef);
                if (deviceCodeRef.get() == null) {
                    hideCopyFields(deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
                }

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

    private void startCountdown(
            long seconds,
            Text details,
            AtomicReference<Timeline> countdownRef,
            DeviceCodeRequestedEvent deviceCodeEvent
    ) {
        stopCountdown(countdownRef);

        final long[] remaining = {Math.max(1, seconds)};
        details.setText(buildDeviceCodePollingMessage(deviceCodeEvent, remaining[0]));

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), _ -> {
            remaining[0]--;
            if (remaining[0] > 0) {
                details.setText(buildDeviceCodePollingMessage(deviceCodeEvent, remaining[0]));
            } else {
                details.setText(buildDeviceCodePollingMessage(deviceCodeEvent, 0));
                stopCountdown(countdownRef);
            }
        }));
        timeline.setCycleCount((int) remaining[0]);
        countdownRef.set(timeline);
        timeline.play();
    }

    private void stopCountdown(AtomicReference<Timeline> countdownRef) {
        Timeline countdown = countdownRef.getAndSet(null);
        if (countdown != null) {
            countdown.stop();
        }
    }

    private String buildDeviceCodeMessage(DeviceCodeRequestedEvent event) {
        if (event == null)
            return "Use the Microsoft device code flow in your browser to continue.";

        if (event.message() != null && !event.message().isBlank())
            return event.message();

        return "Use code " + event.userCode() + " at " + event.verificationUri() + ".";
    }

    private String buildDeviceCodePollingMessage(DeviceCodeRequestedEvent event, long secondsRemaining) {
        if (event == null) {
            return secondsRemaining >= 0
                    ? "Still waiting for Microsoft approval. Checking again in " + secondsRemaining + " seconds."
                    : "Finish the Microsoft device-code step in your browser to continue.";
        }

        StringBuilder builder = new StringBuilder()
                .append("Enter code ")
                .append(event.userCode())
                .append(" at ")
                .append(event.verificationUri())
                .append(".");

        if (secondsRemaining > 0) {
            builder.append(" Checking again in ").append(secondsRemaining).append(" seconds.");
        } else if (secondsRemaining == 0) {
            builder.append(" Checking again now.");
        } else {
            builder.append(" Waiting for approval in your browser.");
        }

        return builder.toString();
    }

    private void showCopyFields(
            DeviceCodeRequestedEvent event,
            HBox deviceCodeRow,
            TextField deviceCodeField,
            HBox verificationUriRow,
            TextField verificationUriField
    ) {
        if (event == null) {
            hideCopyFields(deviceCodeRow, deviceCodeField, verificationUriRow, verificationUriField);
            return;
        }

        deviceCodeField.setText(event.userCode());
        deviceCodeRow.setManaged(true);
        deviceCodeRow.setVisible(true);

        verificationUriField.setText(String.valueOf(event.verificationUri()));
        verificationUriRow.setManaged(true);
        verificationUriRow.setVisible(true);
    }

    private void hideCopyFields(
            HBox deviceCodeRow,
            TextField deviceCodeField,
            HBox verificationUriRow,
            TextField verificationUriField
    ) {
        deviceCodeField.clear();
        deviceCodeRow.setManaged(false);
        deviceCodeRow.setVisible(false);

        verificationUriField.clear();
        verificationUriRow.setManaged(false);
        verificationUriRow.setVisible(false);
    }

    private void copyToClipboard(String value) {
        if (value == null || value.isBlank())
            return;

        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
