package dev.turtywurty.veldtlauncher.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Cursor;
import javafx.scene.Scene;
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
    private static final double RESIZE_MARGIN = 8.0;

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
            if (!event.isPrimaryButtonDown() || isInteractiveTarget(event.getTarget()))
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

    public static void installResizeSupport(Scene scene) {
        var initialBounds = new ResizeBounds();
        var activeEdge = new ResizeEdge[] { ResizeEdge.NONE };

        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            Stage stage = getStage(scene);
            if (stage == null || stage.isMaximized() || activeEdge[0] != ResizeEdge.NONE) {
                scene.setCursor(Cursor.DEFAULT);
                return;
            }

            scene.setCursor(getCursor(resolveResizeEdge(scene, event)));
        });

        scene.addEventFilter(MouseEvent.MOUSE_EXITED, _ -> {
            if (activeEdge[0] == ResizeEdge.NONE) {
                scene.setCursor(Cursor.DEFAULT);
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY)
                return;

            Stage stage = getStage(scene);
            if (stage == null || stage.isMaximized())
                return;

            ResizeEdge edge = resolveResizeEdge(scene, event);
            if (edge == ResizeEdge.NONE)
                return;

            activeEdge[0] = edge;
            initialBounds.capture(stage, event);
            event.consume();
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!event.isPrimaryButtonDown() || activeEdge[0] == ResizeEdge.NONE)
                return;

            Stage stage = getStage(scene);
            if (stage == null || stage.isMaximized()) {
                activeEdge[0] = ResizeEdge.NONE;
                scene.setCursor(Cursor.DEFAULT);
                return;
            }

            resizeStage(stage, scene, event, initialBounds, activeEdge[0]);
            event.consume();
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (activeEdge[0] == ResizeEdge.NONE)
                return;

            activeEdge[0] = ResizeEdge.NONE;
            scene.setCursor(getCursor(resolveResizeEdge(scene, event)));
            event.consume();
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

    private static Stage getStage(Scene scene) {
        if (scene == null)
            return null;

        Window window = scene.getWindow();
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

    private static ResizeEdge resolveResizeEdge(Scene scene, MouseEvent event) {
        boolean left = event.getSceneX() <= RESIZE_MARGIN;
        boolean right = event.getSceneX() >= scene.getWidth() - RESIZE_MARGIN;
        boolean top = event.getSceneY() <= RESIZE_MARGIN;
        boolean bottom = event.getSceneY() >= scene.getHeight() - RESIZE_MARGIN;

        if (top && left) return ResizeEdge.NORTH_WEST;
        if (top && right) return ResizeEdge.NORTH_EAST;
        if (bottom && left) return ResizeEdge.SOUTH_WEST;
        if (bottom && right) return ResizeEdge.SOUTH_EAST;
        if (top) return ResizeEdge.NORTH;
        if (bottom) return ResizeEdge.SOUTH;
        if (left) return ResizeEdge.WEST;
        if (right) return ResizeEdge.EAST;
        return ResizeEdge.NONE;
    }

    private static Cursor getCursor(ResizeEdge edge) {
        return switch (edge) {
            case NORTH -> Cursor.N_RESIZE;
            case SOUTH -> Cursor.S_RESIZE;
            case EAST -> Cursor.E_RESIZE;
            case WEST -> Cursor.W_RESIZE;
            case NORTH_EAST -> Cursor.NE_RESIZE;
            case NORTH_WEST -> Cursor.NW_RESIZE;
            case SOUTH_EAST -> Cursor.SE_RESIZE;
            case SOUTH_WEST -> Cursor.SW_RESIZE;
            case NONE -> Cursor.DEFAULT;
        };
    }

    private static void resizeStage(
            Stage stage,
            Scene scene,
            MouseEvent event,
            ResizeBounds initialBounds,
            ResizeEdge edge
    ) {
        double minWidth = Math.max(stage.getMinWidth(), 0.0);
        double minHeight = Math.max(stage.getMinHeight(), 0.0);
        double maxWidth = stage.getMaxWidth() > 0 ? stage.getMaxWidth() : Double.MAX_VALUE;
        double maxHeight = stage.getMaxHeight() > 0 ? stage.getMaxHeight() : Double.MAX_VALUE;

        double deltaX = event.getScreenX() - initialBounds.screenX;
        double deltaY = event.getScreenY() - initialBounds.screenY;

        if (edge.affectsEast()) {
            double width = clamp(initialBounds.width + deltaX, minWidth, maxWidth);
            stage.setWidth(width);
        }

        if (edge.affectsSouth()) {
            double height = clamp(initialBounds.height + deltaY, minHeight, maxHeight);
            stage.setHeight(height);
        }

        if (edge.affectsWest()) {
            double width = clamp(initialBounds.width - deltaX, minWidth, maxWidth);
            stage.setX(initialBounds.x + (initialBounds.width - width));
            stage.setWidth(width);
        }

        if (edge.affectsNorth()) {
            double height = clamp(initialBounds.height - deltaY, minHeight, maxHeight);
            stage.setY(initialBounds.y + (initialBounds.height - height));
            stage.setHeight(height);
        }

        scene.setCursor(getCursor(edge));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum ResizeEdge {
        NONE,
        NORTH,
        SOUTH,
        EAST,
        WEST,
        NORTH_EAST,
        NORTH_WEST,
        SOUTH_EAST,
        SOUTH_WEST;

        boolean affectsNorth() {
            return this == NORTH || this == NORTH_EAST || this == NORTH_WEST;
        }

        boolean affectsSouth() {
            return this == SOUTH || this == SOUTH_EAST || this == SOUTH_WEST;
        }

        boolean affectsEast() {
            return this == EAST || this == NORTH_EAST || this == SOUTH_EAST;
        }

        boolean affectsWest() {
            return this == WEST || this == NORTH_WEST || this == SOUTH_WEST;
        }
    }

    private static final class ResizeBounds {
        private double x;
        private double y;
        private double width;
        private double height;
        private double screenX;
        private double screenY;

        void capture(Stage stage, MouseEvent event) {
            this.x = stage.getX();
            this.y = stage.getY();
            this.width = stage.getWidth();
            this.height = stage.getHeight();
            this.screenX = event.getScreenX();
            this.screenY = event.getScreenY();
        }
    }
}
