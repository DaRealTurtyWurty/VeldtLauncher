package dev.turtywurty.veldtlauncher.ui.auth;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class AddAccountCard extends StackPane {
    public AddAccountCard() {
        setAlignment(Pos.CENTER);
        setFocusTraversable(true);
        getStyleClass().add("add-account-card");

        var plus = new Label("+");
        plus.getStyleClass().add("add-account-card-plus");

        getChildren().add(plus);
    }
}
