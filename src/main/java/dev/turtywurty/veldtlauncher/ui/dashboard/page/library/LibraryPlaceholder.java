package dev.turtywurty.veldtlauncher.ui.dashboard.page.library;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Objects;

public class LibraryPlaceholder extends VBox {
    public LibraryPlaceholder(Runnable onCreate) {
        Objects.requireNonNull(onCreate, "onCreate");
        getStyleClass().add("library-placeholder");

        var addIcon = new FontIcon(FontAwesomeSolid.PLUS_CIRCLE);
        addIcon.getStyleClass().add("library-placeholder-icon");

        var title = new Label("No items found");
        title.getStyleClass().add("library-placeholder-title");

        var description = new Label("It looks like you haven't added any items to your library yet. Click the button below to get started.");
        description.getStyleClass().add("library-placeholder-description");
        description.setWrapText(true);

        var createButton = new Label("Add Item");
        createButton.getStyleClass().add("library-placeholder-button");
        var createButtonIcon = new FontIcon(FontAwesomeSolid.PLUS);
        createButtonIcon.getStyleClass().add("library-placeholder-button-icon");
        createButton.setGraphic(createButtonIcon);
        createButton.setOnMouseClicked(_ -> onCreate.run());

        getChildren().addAll(addIcon, title, description, createButton);
    }
}
