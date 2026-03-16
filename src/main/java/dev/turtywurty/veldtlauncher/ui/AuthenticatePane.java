package dev.turtywurty.veldtlauncher.ui;

import dev.turtywurty.veldtlauncher.auth.pkce.PkceAuthStrategy;
import dev.turtywurty.veldtlauncher.event.SimpleEventStream;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import org.kordamp.ikonli.fontawesome6.FontAwesomeBrands;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;

public class AuthenticatePane extends AnchorPane {
    public AuthenticatePane() {
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
        button.setOnAction(_ -> {
            var eventStream = new SimpleEventStream();
            var authStrategy = new PkceAuthStrategy(eventStream);
            var scene = getScene();
            var processingPane = new AuthenticationProcessingPane(eventStream, () -> {
                if (scene != null) {
                    scene.setRoot(new AuthenticatePane());
                }
            });

            if (scene != null) {
                scene.setRoot(processingPane);
            }

            button.setDisable(true);
            Thread.startVirtualThread(() -> {
                try {
                    authStrategy.authenticate();
                } catch (Exception ignored) {
                } finally {
                    Platform.runLater(() -> button.setDisable(false));
                }
            });
        });

        card.getChildren().addAll(title, description, button);
        content.getChildren().add(card);

        getChildren().add(content);
    }
}
