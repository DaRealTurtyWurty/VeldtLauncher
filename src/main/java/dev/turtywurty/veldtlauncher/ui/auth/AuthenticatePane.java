package dev.turtywurty.veldtlauncher.ui.auth;

import dev.turtywurty.veldtlauncher.auth.AuthStrategy;
import dev.turtywurty.veldtlauncher.auth.devicecode.DeviceCodeAuthStrategy;
import dev.turtywurty.veldtlauncher.auth.pkce.PkceAuthStrategy;
import dev.turtywurty.veldtlauncher.event.EventStream;
import dev.turtywurty.veldtlauncher.event.SimpleEventStream;
import dev.turtywurty.veldtlauncher.ui.Stylesheets;
import dev.turtywurty.veldtlauncher.ui.WindowChrome;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.kordamp.ikonli.fontawesome6.FontAwesomeBrands;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.concurrent.atomic.AtomicReference;

public class AuthenticatePane extends AnchorPane {
    public AuthenticatePane() {
        this(null);
    }

    public AuthenticatePane(Runnable onBack) {
        super();
        Stylesheets.addAll(this, "authenticate-pane.css", "shared-controls.css");
        getStyleClass().add("authenticate-pane");

        var content = new VBox();
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(48));
        content.getStyleClass().add("authenticate-content");
        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);

        var card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setFillWidth(true);
        card.setMaxWidth(456);
        card.getStyleClass().add("authenticate-card");

        var title = new Text("Authenticate");
        title.getStyleClass().add("authenticate-title");

        var description = new Text("Sign in with Microsoft to access your Minecraft profile.");
        description.getStyleClass().add("authenticate-description");
        description.wrappingWidthProperty().bind(card.maxWidthProperty().subtract(40));

        var microsoftIcon = new FontIcon(FontAwesomeBrands.MICROSOFT);
        microsoftIcon.getStyleClass().add("authenticate-microsoft-icon");
        var button = new Button("Continue with Microsoft", microsoftIcon);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("authenticate-button");
        button.setOnAction(_ -> startAuthentication(new PkceAuthStrategy(new SimpleEventStream())));

        var secureIcon = new FontIcon(FontAwesomeSolid.LOCK);
        secureIcon.getStyleClass().add("authenticate-security-icon");
        var secureTitle = new Text("Browser-based sign-in");
        secureTitle.getStyleClass().add("authenticate-security-title");
        var secureDescription = new Text("The launcher opens Microsoft's sign-in page in your browser. Your password is never handled here.");
        secureDescription.getStyleClass().add("authenticate-security-description");
        var secureCopy = new VBox(4, secureTitle, secureDescription);
        secureCopy.setFillWidth(true);
        HBox.setHgrow(secureCopy, Priority.ALWAYS);
        var secureNote = new HBox(12, secureIcon, secureCopy);
        secureNote.setAlignment(Pos.TOP_LEFT);
        secureNote.getStyleClass().add("authenticate-security-note");

        var havingTroubleSigningIn = new Hyperlink("Having trouble signing in?");
        havingTroubleSigningIn.getStyleClass().add("authenticate-having-trouble");
        havingTroubleSigningIn.setOnAction(_ -> startAuthentication(new DeviceCodeAuthStrategy(new SimpleEventStream())));

        card.getChildren().addAll(
                title,
                description,
                button,
                secureNote,
                havingTroubleSigningIn
        );
        content.getChildren().add(card);

        var backIcon = new FontIcon(FontAwesomeSolid.ARROW_LEFT);
        backIcon.getStyleClass().add("veldt-back-icon");
        var backButton = new Button("Back", backIcon);
        backButton.getStyleClass().add("veldt-back-button");
        backButton.setOnAction(_ -> {
            if (onBack != null) {
                onBack.run();
                return;
            }

            var scene = getScene();
            if (scene != null) {
                scene.setRoot(new PickAccountPane());
            }
        });
        AnchorPane.setTopAnchor(backButton, 24.0);
        AnchorPane.setLeftAnchor(backButton, 24.0);

        var windowBar = new HBox(WindowChrome.createWindowControls(this));
        windowBar.setAlignment(Pos.CENTER_RIGHT);
        windowBar.setPadding(new Insets(12, 12, 0, 12));
        WindowChrome.installDragSupport(windowBar);
        AnchorPane.setTopAnchor(windowBar, 0.0);
        AnchorPane.setLeftAnchor(windowBar, 0.0);
        AnchorPane.setRightAnchor(windowBar, 0.0);

        getChildren().add(content);
        getChildren().add(windowBar);
        getChildren().add(backButton);
    }

    private void startAuthentication(AuthStrategy authStrategy) {
        EventStream eventStream = authStrategy.eventStream();
        Scene scene = getScene();
        AtomicReference<Thread> authThreadRef = new AtomicReference<>();
        var processingPane = new AuthenticationProcessingPane(eventStream, () -> {
            Thread authThread = authThreadRef.getAndSet(null);
            if (authThread != null) {
                authThread.interrupt();
            }

            if (scene != null) {
                scene.setRoot(new AuthenticatePane());
            }
        });

        if (scene != null) {
            scene.setRoot(processingPane);
        }

        Thread authThread = Thread.startVirtualThread(() -> {
            try {
                authStrategy.authenticate();
            } catch (Exception ignored) {
            } finally {
                Platform.runLater(() -> {
                    if (scene != null && scene.getRoot() == processingPane) {
                        processingPane.requestFocus();
                    }
                });
            }
        });
        authThreadRef.set(authThread);
    }
}
