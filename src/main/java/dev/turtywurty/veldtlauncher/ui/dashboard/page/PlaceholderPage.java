package dev.turtywurty.veldtlauncher.ui.dashboard.page;

import dev.turtywurty.veldtlauncher.ui.dashboard.route.RouteId;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class PlaceholderPage extends VeldtPage {
    private final String title;
    private final RouteId routeId;

    public PlaceholderPage(RouteId routeId, String title, String description) {
        this.title = title;
        this.routeId = routeId;

        var content = new VBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getStyleClass().add("dashboard-page-content");

        var titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dashboard-page-title");

        var descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("dashboard-page-description");
        descriptionLabel.setWrapText(true);

        content.getChildren().addAll(titleLabel, descriptionLabel);
        getChildren().add(content);
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public RouteId getRouteId() {
        return this.routeId;
    }
}
