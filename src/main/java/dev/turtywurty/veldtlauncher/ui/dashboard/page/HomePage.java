package dev.turtywurty.veldtlauncher.ui.dashboard.page;

import dev.turtywurty.veldtlauncher.ui.dashboard.route.RouteId;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class HomePage extends VeldtPage {
    public HomePage() {
        var content = new VBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("dashboard-page-content");

        var title = new Label("Home");
        title.getStyleClass().add("dashboard-page-title");

        var description = new Label("Dashboard content will live here.");
        description.getStyleClass().add("dashboard-page-description");
        description.setWrapText(true);

        content.getChildren().addAll(title, description);
        getChildren().add(content);
    }

    @Override
    public String getTitle() {
        return "Home";
    }

    @Override
    public RouteId getRouteId() {
        return RouteId.HOME;
    }
}
