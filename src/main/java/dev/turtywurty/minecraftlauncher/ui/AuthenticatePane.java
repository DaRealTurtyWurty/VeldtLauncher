package dev.turtywurty.minecraftlauncher.ui;

import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.kordamp.ikonli.fontawesome6.FontAwesomeBrands;
import org.kordamp.ikonli.javafx.FontIcon;

public class AuthenticatePane extends AnchorPane {
    public AuthenticatePane() {
        super();
        getStyleClass().add("authenticate-pane");

        var content = new VBox(5);
        content.getStyleClass().add("authenticate-content");
        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);

        var title = new Text("Authenticate");
        title.getStyleClass().add("authenticate-title");
        content.getChildren().add(title);

        var description = new Text("Please sign in to your Minecraft account to continue.");
        description.getStyleClass().add("authenticate-description");
        content.getChildren().add(description);

        var microsoftIcon = new FontIcon(FontAwesomeBrands.MICROSOFT);
        microsoftIcon.getStyleClass().add("authenticate-microsoft-icon");
        var button = new Button("Continue with Microsoft", microsoftIcon);
        button.getStyleClass().add("authenticate-button");
        content.getChildren().add(button);

        getChildren().add(content);
    }
}
