package dev.turtywurty.veldtlauncher.ui.dashboard.page;

import dev.turtywurty.veldtlauncher.ui.dashboard.route.RouteId;
import javafx.scene.layout.StackPane;

public abstract class VeldtPage extends StackPane {
    protected VeldtPage() {
        getStyleClass().add("veldt-page");
    }

    public abstract String getTitle();

    public abstract RouteId getRouteId();

    public boolean canNavigateAway() {
        return true;
    }
}
