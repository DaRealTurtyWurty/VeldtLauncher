package dev.turtywurty.veldtlauncher.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome6.FontAwesomeRegular;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

public final class WindowChrome {
    private WindowChrome() {
    }

    public static HBox createWindowControls(Node owner) {
        var minimizeButton = createWindowButton(FontAwesomeSolid.WINDOW_MINIMIZE, "window-control-minimize-button");
        minimizeButton.setOnAction(_ -> withStage(owner, stage -> stage.setIconified(true)));

        var maximizeButton = createWindowButton(FontAwesomeRegular.WINDOW_MAXIMIZE, "window-control-maximize-button");
        maximizeButton.setOnAction(_ -> withStage(owner, stage -> stage.setMaximized(!stage.isMaximized())));
        owner.sceneProperty().addListener((_, _, newScene) -> {
            if (newScene == null)
                return;

            Window window = newScene.getWindow();
            if (window instanceof Stage stage) {
                updateMaximizeGraphic(maximizeButton, stage.isMaximized());
                stage.maximizedProperty().addListener((_, _, maximized) ->
                        updateMaximizeGraphic(maximizeButton, maximized));
            }
        });

        var closeButton = createWindowButton(FontAwesomeSolid.TIMES, "window-control-close-button");
        closeButton.setOnAction(_ -> withStage(owner, Stage::close));

        var controls = new HBox(6, minimizeButton, maximizeButton, closeButton);
        controls.setAlignment(Pos.CENTER_RIGHT);
        controls.getStyleClass().add("window-controls");
        return controls;
    }

    public static void installDragSupport(Node dragRegion) {
        final double[] offsetX = new double[1];
        final double[] offsetY = new double[1];

        dragRegion.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || isInteractiveTarget(event.getTarget()))
                return;

            Stage stage = getStage(dragRegion);
            if (stage == null)
                return;

            offsetX[0] = event.getScreenX() - stage.getX();
            offsetY[0] = event.getScreenY() - stage.getY();
        });

        dragRegion.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || isInteractiveTarget(event.getTarget()))
                return;

            Stage stage = getStage(dragRegion);
            if (stage == null || stage.isMaximized())
                return;

            stage.setX(event.getScreenX() - offsetX[0]);
            stage.setY(event.getScreenY() - offsetY[0]);
        });

        dragRegion.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2 || isInteractiveTarget(event.getTarget()))
                return;

            Stage stage = getStage(dragRegion);
            if (stage != null) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
    }

    private static Button createWindowButton(Ikon icon, String styleClass) {
        var graphic = new FontIcon(icon);
        graphic.getStyleClass().add("window-control-icon");

        var button = new Button(null, graphic);
        button.getStyleClass().add("window-control-button");
        button.getStyleClass().add(styleClass);
        button.setFocusTraversable(false);
        return button;
    }

    private static void updateMaximizeGraphic(Button button, boolean maximized) {
        var graphic = new FontIcon(maximized ? FontAwesomeRegular.WINDOW_RESTORE : FontAwesomeRegular.WINDOW_MAXIMIZE);
        graphic.getStyleClass().add("window-control-icon");
        button.setGraphic(graphic);
    }

    private static void withStage(Node owner, java.util.function.Consumer<Stage> consumer) {
        Stage stage = getStage(owner);
        if (stage != null) {
            consumer.accept(stage);
        }
    }

    private static Stage getStage(Node node) {
        if (node == null || node.getScene() == null)
            return null;

        Window window = node.getScene().getWindow();
        return window instanceof Stage stage ? stage : null;
    }

    private static boolean isInteractiveTarget(Object target) {
        if (!(target instanceof Node node))
            return false;

        Node current = node;
        while (current != null) {
            if (current instanceof ButtonBase || current instanceof TextInputControl)
                return true;

            current = current.getParent();
        }

        return false;
    }
}
