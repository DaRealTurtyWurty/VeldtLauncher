package dev.turtywurty.veldtlauncher.ui.auth;

import dev.turtywurty.veldtlauncher.auth.AuthEvent;
import dev.turtywurty.veldtlauncher.auth.browser.DesktopBrowserOpener;
import dev.turtywurty.veldtlauncher.auth.devicecode.event.*;
import dev.turtywurty.veldtlauncher.auth.event.*;
import dev.turtywurty.veldtlauncher.event.EventListener;
import dev.turtywurty.veldtlauncher.event.EventStream;
import dev.turtywurty.veldtlauncher.ui.Stylesheets;
import dev.turtywurty.veldtlauncher.ui.WindowChrome;
import dev.turtywurty.veldtlauncher.ui.dashboard.shell.DashboardShell;
import dev.turtywurty.veldtlauncher.util.QrCodeUtil;
import io.nayuki.qrcodegen.QrCode;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicReference;

public class AuthenticationProcessingPane extends AnchorPane {
    private static final int DEVICE_CODE_LENGTH = 6;

    private final Label clipboardToast = new Label("Copied to clipboard");
    private final SequentialTransition clipboardToastAnimation;

    public AuthenticationProcessingPane(EventStream eventStream, Runnable onBack) {
        Stylesheets.addAll(this, "authenticate-processing-pane.css", "shared-controls.css");

        getStyleClass().add("authentication-processing-pane");

        this.clipboardToast.getStyleClass().add("authentication-processing-toast");
        this.clipboardToast.setManaged(false);
        this.clipboardToast.setVisible(false);
        this.clipboardToast.setOpacity(0);

        var toastFadeIn = new FadeTransition(Duration.millis(140), this.clipboardToast);
        toastFadeIn.setFromValue(0);
        toastFadeIn.setToValue(1);
        var toastPause = new PauseTransition(Duration.seconds(1.5));
        var toastFadeOut = new FadeTransition(Duration.millis(220), this.clipboardToast);
        toastFadeOut.setFromValue(1);
        toastFadeOut.setToValue(0);
        this.clipboardToastAnimation = new SequentialTransition(toastFadeIn, toastPause, toastFadeOut);
        this.clipboardToastAnimation.setOnFinished(_ -> {
            this.clipboardToast.setManaged(false);
            this.clipboardToast.setVisible(false);
        });

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

        Label[] deviceCodeBoxes = new Label[DEVICE_CODE_LENGTH];
        var deviceCodeRow = new HBox(8);
        deviceCodeRow.setAlignment(Pos.CENTER);
        deviceCodeRow.getStyleClass().add("authentication-processing-device-code-row");
        Tooltip.install(deviceCodeRow, new Tooltip("Click to copy device code"));
        for (int index = 0; index < DEVICE_CODE_LENGTH; index++) {
            var codeBox = new Label();
            codeBox.setMinSize(44, 52);
            codeBox.setPrefSize(44, 52);
            codeBox.setMaxSize(44, 52);
            codeBox.setAlignment(Pos.CENTER);
            codeBox.getStyleClass().add("authentication-processing-device-code-box");
            deviceCodeBoxes[index] = codeBox;
            deviceCodeRow.getChildren().add(codeBox);
        }
        deviceCodeRow.setManaged(false);
        deviceCodeRow.setVisible(false);

        var qrCodeImageView = new ImageView();
        qrCodeImageView.setFitWidth(128);
        qrCodeImageView.setFitHeight(128);

        var verificationUriLink = new Hyperlink();
        verificationUriLink.setFocusTraversable(false);
        verificationUriLink.getStyleClass().add("authentication-processing-link");

        var verificationUriRow = new HBox(verificationUriLink);
        verificationUriRow.setAlignment(Pos.CENTER);
        verificationUriRow.setManaged(false);
        verificationUriRow.setVisible(false);
        verificationUriRow.getStyleClass().add("authentication-processing-link-row");

        var backButton = new Button("Back");
        backButton.getStyleClass().add("authentication-processing-back-button");
        backButton.setManaged(false);
        backButton.setVisible(false);
        backButton.setOnAction(_ -> {
            if (onBack != null) {
                onBack.run();
            }
        });

        card.getChildren().addAll(indicator, title, status, details, deviceCodeRow, qrCodeImageView, verificationUriRow, backButton);
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

        var toastRow = new HBox(this.clipboardToast);
        toastRow.setMouseTransparent(true);
        toastRow.setAlignment(Pos.CENTER);
        AnchorPane.setLeftAnchor(toastRow, 0.0);
        AnchorPane.setRightAnchor(toastRow, 0.0);
        AnchorPane.setBottomAnchor(toastRow, 24.0);
        getChildren().add(toastRow);

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
                            deviceCodeBoxes,
                            qrCodeImageView,
                            verificationUriRow,
                            verificationUriLink,
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
            Label[] deviceCodeBoxes,
            ImageView qrCodeImageView,
            HBox verificationUriRow,
            Hyperlink verificationUriLink,
            Button backButton,
            AtomicReference<Timeline> countdownRef,
            AtomicReference<DeviceCodeRequestedEvent> deviceCodeRef
    ) {
        switch (event) {
            case AuthenticationStartedEvent ignored -> {
                stopCountdown(countdownRef);
                deviceCodeRef.set(null);
                hideDeviceCodeFields(deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                title.setText("Authenticating...");
                status.setText("Preparing your secure Microsoft login.");
                details.setText("The launcher is setting up a local callback so it can finish sign-in.");
                setBackButtonVisible(backButton, false);
            }
            case DeviceCodeRequestedEvent deviceCodeEvent -> {
                stopCountdown(countdownRef);
                deviceCodeRef.set(deviceCodeEvent);
                showDeviceCodeFields(deviceCodeEvent, deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                status.setText("Device code ready.");
                details.setText(buildDeviceCodeMessage(deviceCodeEvent));
                setBackButtonVisible(backButton, true);
            }
            case DeviceCodePollingStartedEvent _ -> {
                stopCountdown(countdownRef);
                showDeviceCodeFields(deviceCodeRef.get(), deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                status.setText("Waiting for device-code approval...");
                details.setText(buildDeviceCodePollingMessage(deviceCodeRef.get(), -1));
                setBackButtonVisible(backButton, true);
            }
            case DeviceCodeAuthorizationPendingEvent deviceCodeEvent -> {
                showDeviceCodeFields(deviceCodeRef.get(), deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                status.setText("Waiting for device-code approval...");
                startCountdown(deviceCodeEvent.pollingIntervalSeconds(), details, countdownRef, deviceCodeRef.get());
                setBackButtonVisible(backButton, true);
            }
            case DeviceCodeAuthorizationDeclinedEvent ignored -> {
                stopCountdown(countdownRef);
                showDeviceCodeFields(deviceCodeRef.get(), deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                status.setText("Device-code authorization declined.");
                details.setText("The Microsoft device-code request was declined before authentication could finish.");
                setBackButtonVisible(backButton, true);
            }
            case DeviceCodeSucceededEvent ignored -> {
                stopCountdown(countdownRef);
                hideDeviceCodeFields(deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                status.setText("Microsoft device-code login complete.");
                details.setText("Requesting Xbox Live credentials for your Microsoft account.");
                setBackButtonVisible(backButton, false);
            }
            case CallbackServerStartedEvent callbackEvent -> {
                stopCountdown(countdownRef);
                hideDeviceCodeFields(deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                status.setText("Callback server is ready.");
                details.setText("Listening on " + callbackEvent.redirectUri() + " for the Microsoft redirect.");
                setBackButtonVisible(backButton, false);
            }
            case OpeningBrowserEvent ignored -> {
                status.setText("Opening browser...");
                DeviceCodeRequestedEvent deviceCodeEvent = deviceCodeRef.get();
                if (deviceCodeEvent != null) {
                    showDeviceCodeFields(deviceCodeEvent, deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                    setBackButtonVisible(backButton, true);
                } else {
                    hideDeviceCodeFields(deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                    setBackButtonVisible(backButton, false);
                }
                details.setText(deviceCodeEvent != null
                        ? buildDeviceCodePollingMessage(deviceCodeEvent, -1)
                        : "Approve the sign-in request in your browser to continue.");
            }
            case WaitingForCallbackEvent ignored -> {
                stopCountdown(countdownRef);
                hideDeviceCodeFields(deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                status.setText("Waiting for Microsoft callback...");
                details.setText("Finish sign-in in your browser, then return here while the launcher receives the response.");
                setBackButtonVisible(backButton, false);
            }
            case AuthorizationCallbackReceivedEvent callbackEvent -> {
                stopCountdown(countdownRef);
                hideDeviceCodeFields(deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                status.setText(callbackEvent.error()
                        ? "Microsoft returned an error."
                        : "Authorization callback received.");
                details.setText(callbackEvent.error()
                        ? "The launcher received an error response from the Microsoft sign-in flow."
                        : "Continuing with Xbox and Minecraft authentication.");
                setBackButtonVisible(backButton, false);
            }
            case MicrosoftLoginSucceededEvent ignored -> {
                stopCountdown(countdownRef);
                hideDeviceCodeFields(deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                status.setText("Microsoft login complete.");
                details.setText("Requesting Xbox Live credentials for your Microsoft account.");
                setBackButtonVisible(backButton, false);
            }
            case XboxAuthStartedEvent ignored -> {
                stopCountdown(countdownRef);
                hideDeviceCodeFields(deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                status.setText("Authorizing with Xbox Live...");
                details.setText("Exchanging your Microsoft session for Xbox and XSTS tokens.");
                setBackButtonVisible(backButton, false);
            }
            case MinecraftProfileFetchedEvent profileEvent -> {
                stopCountdown(countdownRef);
                hideDeviceCodeFields(deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                title.setText("Minecraft profile found.");
                status.setText("Signed in as " + profileEvent.username() + ".");
                details.setText("Profile " + profileEvent.uuid() + " was fetched successfully.");
                setBackButtonVisible(backButton, false);
            }
            case AuthenticationSucceededEvent succeededEvent -> {
                stopCountdown(countdownRef);
                hideDeviceCodeFields(deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                indicator.setProgress(1);
                title.setText("Authentication complete.");
                status.setText("Welcome, " + succeededEvent.username() + ".");
                details.setText("Your Minecraft session is ready.");
                setBackButtonVisible(backButton, false);
                Scene scene = getScene();
                if (scene != null) {
                    DashboardShell.show(scene);
                }
            }
            case AuthenticationFailedEvent failedEvent -> {
                stopCountdown(countdownRef);
                if (deviceCodeRef.get() == null) {
                    hideDeviceCodeFields(deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
                }

                if (!indicator.getStyleClass().contains("authentication-progress-indicator-failed")) {
                    indicator.getStyleClass().add("authentication-progress-indicator-failed");
                }

                title.setText("Authentication failed.");
                status.setText(failedEvent.message());
                details.setText("Try again after fixing the underlying issue.");
                setBackButtonVisible(backButton, true);
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

    private void showDeviceCodeFields(
            DeviceCodeRequestedEvent event,
            HBox deviceCodeRow,
            Label[] deviceCodeBoxes,
            ImageView qrCodeImageView, HBox verificationUriRow,
            Hyperlink verificationUriLink
    ) {
        if (event == null) {
            hideDeviceCodeFields(deviceCodeRow, deviceCodeBoxes, qrCodeImageView, verificationUriRow, verificationUriLink);
            return;
        }

        populateDeviceCodeBoxes(deviceCodeBoxes, event.userCode());
        deviceCodeRow.setOnMouseClicked(_ -> copyToClipboard(event.userCode()));
        deviceCodeRow.setManaged(true);
        deviceCodeRow.setVisible(true);

        String uri = String.valueOf(
                event.verificationUriComplete() != null ? event.verificationUriComplete() : event.verificationUri()
        );
        QrCode qrCode = QrCodeUtil.generateQrCode(uri, QrCode.Ecc.MEDIUM);
        qrCodeImageView.setImage(QrCodeUtil.toFxImage(qrCode, 124, 2));
        qrCodeImageView.setManaged(true);
        qrCodeImageView.setVisible(true);

        verificationUriLink.setText(String.valueOf(event.verificationUri()));
        verificationUriLink.setOnAction(_ -> {
            var browserOpener = new DesktopBrowserOpener();
            if (browserOpener.isSupported()) {
                browserOpener.open(event.verificationUri());
            }
        });
        verificationUriRow.setManaged(true);
        verificationUriRow.setVisible(true);
    }

    private void hideDeviceCodeFields(
            HBox deviceCodeRow,
            Label[] deviceCodeBoxes,
            ImageView qrCodeImageView,
            HBox verificationUriRow,
            Hyperlink verificationUriLink
    ) {
        clearDeviceCodeBoxes(deviceCodeBoxes);
        deviceCodeRow.setOnMouseClicked(null);
        deviceCodeRow.setManaged(false);
        deviceCodeRow.setVisible(false);

        qrCodeImageView.setImage(null);
        qrCodeImageView.setManaged(false);
        qrCodeImageView.setVisible(false);

        verificationUriLink.setText("");
        verificationUriLink.setOnAction(null);
        verificationUriRow.setManaged(false);
        verificationUriRow.setVisible(false);
    }

    private void populateDeviceCodeBoxes(Label[] deviceCodeBoxes, String userCode) {
        clearDeviceCodeBoxes(deviceCodeBoxes);
        if (userCode == null || userCode.isBlank())
            return;

        String normalizedCode = userCode.replaceAll("[^A-Za-z0-9]", "");
        for (int index = 0; index < Math.min(deviceCodeBoxes.length, normalizedCode.length()); index++) {
            deviceCodeBoxes[index].setText(String.valueOf(normalizedCode.charAt(index)).toUpperCase());
        }
    }

    private void clearDeviceCodeBoxes(Label[] deviceCodeBoxes) {
        for (Label deviceCodeBox : deviceCodeBoxes) {
            deviceCodeBox.setText("");
        }
    }

    private void setBackButtonVisible(Button backButton, boolean visible) {
        backButton.setManaged(visible);
        backButton.setVisible(visible);
    }

    private void copyToClipboard(String value) {
        if (value == null || value.isBlank())
            return;

        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
        showClipboardToast("Device code copied to clipboard");
    }

    private void showClipboardToast(String message) {
        this.clipboardToastAnimation.stop();
        this.clipboardToast.setText(message);
        this.clipboardToast.setManaged(true);
        this.clipboardToast.setVisible(true);
        this.clipboardToast.setOpacity(0);
        this.clipboardToastAnimation.playFromStart();
    }
}
