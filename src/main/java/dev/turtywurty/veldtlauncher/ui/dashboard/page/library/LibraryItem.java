package dev.turtywurty.veldtlauncher.ui.dashboard.page.library;

import dev.turtywurty.veldtlauncher.ui.Images;
import dev.turtywurty.veldtlauncher.ui.dashboard.navigation.Navigator;
import dev.turtywurty.veldtlauncher.ui.dashboard.shell.DashboardShell;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.nio.file.Path;

public class LibraryItem<T extends LibraryItemContent> extends HBox {
    private static final String DEFAULT_INSTANCE_IMAGE = "minecraft_cover_art.png";
    private final T content;

    public LibraryItem(T content) {
        this.content = content;
        getStyleClass().add("library-item");

        var title = new Text(content.getName());
        title.getStyleClass().add("library-item-title");

        var iconContainer = new StackPane();
        iconContainer.getStyleClass().add("library-item-icon-container");
        iconContainer.getChildren().add(createIconNode(content));

        var description = new Text(content.getDescription());
        description.getStyleClass().add("library-item-description");

        var textContainer = new VBox(title, description);
        textContainer.getStyleClass().add("library-item-text");
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane actionContainer = createActionContainer();

        setOnMouseClicked(_ -> runItemAction());

        getChildren().addAll(iconContainer, textContainer, spacer, actionContainer);
    }

    private StackPane createActionContainer() {
        Button playButton = new Button();
        playButton.getStyleClass().add("library-item-play-button");
        playButton.setGraphic(createPlayIcon());
        playButton.setText(null);
        playButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        playButton.setFocusTraversable(false);
        playButton.setMouseTransparent(true);
        playButton.setOnMouseClicked(event -> event.consume());
        playButton.setOnAction(event -> {
            event.consume();
            runPlayAction();
        });

        StackPane actionContainer = new StackPane(playButton);
        actionContainer.getStyleClass().add("library-item-action-container");
        actionContainer.setManaged(true);
        actionContainer.setVisible(true);

        hoverProperty().addListener((_, _, hovered) -> {
            playButton.setMouseTransparent(!hovered);
        });

        return actionContainer;
    }

    private Node createPlayIcon() {
        FontIcon icon = new FontIcon(FontAwesomeSolid.PLAY);
        icon.getStyleClass().add("library-item-play-icon");
        return icon;
    }

    private Node createIconNode(T content) {
        String iconPath = content.getIconPath();
        Image image = loadImage(iconPath);
        if (image == null || image.isError()) {
            image = loadDefaultImage();
        }

        if (image != null && !image.isError()) {
            var icon = new ImageView(image);
            icon.setFitWidth(32);
            icon.setFitHeight(32);
            icon.setPreserveRatio(true);
            icon.getStyleClass().add("library-item-icon");
            return icon;
        }

        String iconLetter = content.getName().isBlank() ? "?" : String.valueOf(Character.toUpperCase(content.getName().charAt(0)));
        var fallback = new Label(iconLetter);
        fallback.getStyleClass().add("library-item-icon-fallback");
        return fallback;
    }

    private Image loadImage(String iconPath) {
        if (iconPath == null || iconPath.isBlank()) {
            return null;
        }

        try {
            return new Image(iconPath, 32, 32, true, true);
        } catch (IllegalArgumentException ignored) {
        }

        try {
            return new Image(Path.of(iconPath).toUri().toString(), 32, 32, true, true);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Image loadDefaultImage() {
        try {
            return Images.load(DEFAULT_INSTANCE_IMAGE, 32, 32, true, true);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private DashboardShell resolveShell() {
        Parent parent = getParent();
        while (parent != null) {
            if (parent instanceof DashboardShell shell)
                return shell;

            parent = parent.getParent();
        }

        if (getScene() != null && getScene().getRoot() instanceof DashboardShell shell)
            return shell;

        return null;
    }

    private Navigator resolveNavigator() {
        DashboardShell shell = resolveShell();
        return shell == null ? null : shell.getNavigator();
    }

    private void runItemAction() {
        Navigator navigator = resolveNavigator();
        if (navigator != null) {
            this.content.getOnClickAction().accept(navigator);
        }
    }

    private void runPlayAction() {
        DashboardShell shell = resolveShell();
        if (shell != null) {
            this.content.onPlay(shell.getNavigator(), shell);
            return;
        }

        Navigator navigator = resolveNavigator();
        if (navigator != null) {
            this.content.getOnPlayAction().accept(navigator);
        }
    }

    public T getContent() {
        return this.content;
    }
}
