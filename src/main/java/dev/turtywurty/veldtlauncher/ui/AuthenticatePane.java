package dev.turtywurty.veldtlauncher.ui;

import dev.turtywurty.veldtlauncher.auth.AuthStrategy;
import dev.turtywurty.veldtlauncher.auth.devicecode.DeviceCodeAuthStrategy;
import dev.turtywurty.veldtlauncher.auth.pkce.PkceAuthStrategy;
import dev.turtywurty.veldtlauncher.event.EventStream;
import dev.turtywurty.veldtlauncher.event.SimpleEventStream;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.kordamp.ikonli.fontawesome6.FontAwesomeBrands;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;

public class AuthenticatePane extends AnchorPane {
    public AuthenticatePane() {
        this(null);
    }

    public AuthenticatePane(Runnable onBack) {
        super();
        getStylesheets().add(Objects.requireNonNull(
                AuthenticatePane.class.getResource("authenticate-pane.css"),
                "Missing stylesheet: authenticate-pane.css"
        ).toExternalForm());
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
        card.setMaxWidth(420);
        card.getStyleClass().add("authenticate-card");

        var title = new Text("Authenticate");
        title.getStyleClass().add("authenticate-title");

        var description = new Text("Please sign in to your Minecraft account to continue.");
        description.getStyleClass().add("authenticate-description");
        description.wrappingWidthProperty().bind(card.maxWidthProperty().subtract(56));

        var microsoftIcon = new FontIcon(FontAwesomeBrands.MICROSOFT);
        microsoftIcon.getStyleClass().add("authenticate-microsoft-icon");
        var button = new Button("Continue with Microsoft", microsoftIcon);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("authenticate-button");
        button.setOnAction(_ -> startAuthentication(new PkceAuthStrategy(new SimpleEventStream())));

        var havingTroubleSigningIn = new Hyperlink("Having trouble signing in?");
        havingTroubleSigningIn.getStyleClass().add("authenticate-having-trouble");
        havingTroubleSigningIn.setOnAction(_ -> startAuthentication(new DeviceCodeAuthStrategy(new SimpleEventStream())));

        card.getChildren().addAll(title, description, button, havingTroubleSigningIn);
        content.getChildren().add(card);

        var backIcon = new FontIcon(FontAwesomeSolid.ARROW_LEFT);
        backIcon.getStyleClass().add("authenticate-back-icon");
        var backButton = new Button("Back", backIcon);
        backButton.getStyleClass().add("authenticate-back-button");
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

        getChildren().add(content);
        getChildren().add(backButton);
    }

    private void startAuthentication(AuthStrategy authStrategy) {
        EventStream eventStream = authStrategy.eventStream();
        Scene scene = getScene();
        var processingPane = new AuthenticationProcessingPane(eventStream, () -> {
            if (scene != null) {
                scene.setRoot(new AuthenticatePane());
            }
        });

        if (scene != null) {
            scene.setRoot(processingPane);
        }

        Thread.startVirtualThread(() -> {
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
    }
}
