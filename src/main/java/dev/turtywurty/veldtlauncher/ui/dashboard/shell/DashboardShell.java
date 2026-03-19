package dev.turtywurty.veldtlauncher.ui.dashboard.shell;

import dev.turtywurty.veldtlauncher.ui.Stylesheets;
import dev.turtywurty.veldtlauncher.ui.WindowChrome;
import dev.turtywurty.veldtlauncher.ui.auth.PickAccountPane;
import dev.turtywurty.veldtlauncher.ui.dashboard.navigation.DefaultNavigator;
import dev.turtywurty.veldtlauncher.ui.dashboard.navigation.Navigator;
import dev.turtywurty.veldtlauncher.ui.dashboard.page.VeldtPage;
import dev.turtywurty.veldtlauncher.ui.dashboard.route.RouteId;
import dev.turtywurty.veldtlauncher.ui.dashboard.route.RouteRegistry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DashboardShell extends BorderPane {
    private static final double SIDEBAR_COLLAPSED_WIDTH = 76;

    private final Navigator navigator;
    private final VBox sidebar = new VBox(12);
    private final StackPane appIcon = new StackPane();
    private final Label appTitleLabel = new Label("VeldtLauncher");
    private final Button previousButton = new Button();
    private final Button nextButton = new Button();
    private final HBox windowControls = WindowChrome.createWindowControls(this);
    private final Label titleLabel = new Label();
    private final Map<SidebarItem, Button> sidebarButtons = new EnumMap<>(SidebarItem.class);
    private final Map<Button, Popup> buttonTooltips = new HashMap<>();
    private RouteId currentRouteId;

    public DashboardShell() {
        this(new DefaultNavigator(RouteId.LIBRARY_ALL));
    }

    public DashboardShell(Navigator navigator) {
        this.navigator = Objects.requireNonNull(navigator, "navigator");
        Stylesheets.addAll(this, "shared-controls.css", "dashboard-shell.css");
        getStyleClass().add("dashboard-shell");
        configureSidebar();
        configureTopBar();
        setCurrentPage(this.navigator.getCurrentPage());
        updateNavigationButtons();
        this.navigator.addNavigationListener((_, newRouteId, _) -> {
            setCurrentPage(newRouteId);
            updateNavigationButtons();
        });
    }

    public static void show(Scene scene) {
        Objects.requireNonNull(scene, "scene");
        scene.setRoot(new DashboardShell());
    }

    private void configureTopBar() {
        var appIconLabel = new Label("V");
        appIconLabel.getStyleClass().add("dashboard-app-icon-label");
        this.appIcon.getChildren().setAll(appIconLabel);
        this.appIcon.getStyleClass().add("dashboard-app-icon");

        this.appTitleLabel.getStyleClass().add("dashboard-app-title");

        var previousIcon = new FontIcon(FontAwesomeSolid.ARROW_LEFT);
        previousIcon.getStyleClass().add("veldt-back-icon");
        this.previousButton.setGraphic(previousIcon);
        this.previousButton.getStyleClass().add("veldt-back-button");
        this.previousButton.getStyleClass().add("dashboard-history-button");
        this.previousButton.setOnAction(_ -> this.navigator.navigateBack());

        var nextIcon = new FontIcon(FontAwesomeSolid.ARROW_RIGHT);
        nextIcon.getStyleClass().add("veldt-back-icon");
        this.nextButton.setGraphic(nextIcon);
        this.nextButton.getStyleClass().add("veldt-back-button");
        this.nextButton.getStyleClass().add("dashboard-history-button");
        this.nextButton.setOnAction(_ -> this.navigator.navigateForward());

        this.titleLabel.getStyleClass().add("dashboard-title");

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var historyButtons = new HBox(6, this.previousButton, this.nextButton);
        historyButtons.setAlignment(Pos.CENTER_LEFT);

        var topBar = new HBox(12,
                this.appIcon,
                this.appTitleLabel,
                historyButtons,
                this.titleLabel,
                spacer,
                this.windowControls
        );
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16));
        topBar.getStyleClass().add("dashboard-top-bar");
        WindowChrome.installDragSupport(topBar);

        setTop(topBar);
    }

    private void configureSidebar() {
        var primaryNav = new VBox(8,
                createRouteButton("Library", SidebarItem.LIBRARY, RouteId.LIBRARY_ALL, FontAwesomeSolid.BOOK),
                createRouteButton("Discover", SidebarItem.DISCOVER, RouteId.DISCOVER_MODPACKS, FontAwesomeSolid.COMPASS),
                createSidebarSeparator(),
                createPrimaryRouteButton("Add Instance", SidebarItem.ADD_INSTANCE, RouteId.ADD_INSTANCE, FontAwesomeSolid.PLUS)
        );
        primaryNav.getStyleClass().add("dashboard-sidebar-group");

        var spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        var bottomNav = new VBox(8,
                createRouteButton("Settings", SidebarItem.SETTINGS, RouteId.SETTINGS, FontAwesomeSolid.COG),
                createRouteButton("Profile", SidebarItem.PROFILE, RouteId.VIEW_PROFILE, FontAwesomeSolid.USER),
                createActionButton("Sign Out", FontAwesomeSolid.SIGN_OUT_ALT, this::signOut, "dashboard-nav-button-danger")
        );
        bottomNav.getStyleClass().add("dashboard-sidebar-group");

        this.sidebar.getChildren().setAll(primaryNav, spacer, bottomNav);
        this.sidebar.getStyleClass().add("dashboard-sidebar");
        this.sidebar.setFillWidth(false);
        this.sidebar.setPrefWidth(SIDEBAR_COLLAPSED_WIDTH);
        this.sidebar.setMinWidth(SIDEBAR_COLLAPSED_WIDTH);
        this.sidebar.setMaxWidth(SIDEBAR_COLLAPSED_WIDTH);

        BorderPane.setAlignment(this.sidebar, Pos.CENTER_LEFT);
        BorderPane.setMargin(this.sidebar, new Insets(16, 0, 16, 16));
        setLeft(this.sidebar);
    }

    private Button createRouteButton(String text, SidebarItem sidebarItem, RouteId routeId, FontAwesomeSolid icon) {
        return createRouteButton(text, sidebarItem, routeId, icon, false);
    }

    private Button createPrimaryRouteButton(String text, SidebarItem sidebarItem, RouteId routeId, FontAwesomeSolid icon) {
        return createRouteButton(text, sidebarItem, routeId, icon, true);
    }

    private Button createRouteButton(
            String text,
            SidebarItem sidebarItem,
            RouteId routeId,
            FontAwesomeSolid icon,
            boolean primary
    ) {
        var button = createIconButton(text, icon, null);
        button.getStyleClass().add("dashboard-nav-button");
        if (primary) {
            button.getStyleClass().add("dashboard-nav-button-primary");
        }

        button.setOnAction(_ -> this.navigator.navigateTo(routeId));
        this.sidebarButtons.put(sidebarItem, button);
        return button;
    }

    private Button createActionButton(String text, FontAwesomeSolid icon, Runnable action, String extraStyleClass) {
        var button = createIconButton(text, icon, extraStyleClass);
        button.getStyleClass().add("dashboard-nav-button");
        if (extraStyleClass != null && !extraStyleClass.isBlank()) {
            button.getStyleClass().add(extraStyleClass);
        }

        button.setOnAction(_ -> action.run());
        return button;
    }

    private Button createIconButton(String text, FontAwesomeSolid icon, String extraStyleClass) {
        var fontIcon = new FontIcon(icon);
        fontIcon.getStyleClass().add("dashboard-nav-icon");
        if (extraStyleClass != null && !extraStyleClass.isBlank()) {
            fontIcon.getStyleClass().add(extraStyleClass + "-icon");
        }

        var button = new Button(null, fontIcon);
        var tooltip = createSidebarTooltip(text);

        this.buttonTooltips.put(button, tooltip);
        button.setAccessibleText(text);
        button.setMnemonicParsing(false);
        button.setPrefHeight(44);
        button.setMinHeight(44);
        button.setMaxHeight(44);
        button.setMinWidth(44);
        button.setPrefWidth(44);
        button.setMaxWidth(44);
        button.setAlignment(Pos.CENTER);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setOnMouseEntered(_ -> showTooltip(button));
        button.setOnMouseExited(_ -> hideTooltip(button));
        button.setOnMousePressed(_ -> hideTooltip(button));
        return button;
    }

    private void setCurrentPage(RouteId routeId) {
        RouteId currentRouteId = Objects.requireNonNull(routeId, "routeId");
        VeldtPage page = RouteRegistry.INSTANCE.getRouteContent(currentRouteId);
        this.currentRouteId = currentRouteId;
        this.titleLabel.setText(page.getTitle());

        BorderPane.setAlignment(page, Pos.TOP_LEFT);
        BorderPane.setMargin(page, new Insets(16, 16, 16, 16));
        setCenter(page);
    }

    private void updateNavigationButtons() {
        this.previousButton.setDisable(!this.navigator.canNavigateBack());
        this.nextButton.setDisable(!this.navigator.canNavigateForward());

        for (Map.Entry<SidebarItem, Button> entry : this.sidebarButtons.entrySet()) {
            Button button = entry.getValue();
            boolean selected = isSelected(entry.getKey(), this.currentRouteId);
            if (selected) {
                if (!button.getStyleClass().contains("dashboard-nav-button-selected")) {
                    button.getStyleClass().add("dashboard-nav-button-selected");
                }
            } else {
                button.getStyleClass().remove("dashboard-nav-button-selected");
            }
        }
    }

    private boolean isSelected(SidebarItem sidebarItem, RouteId activeRouteId) {
        return sidebarItem != null
                && activeRouteId != null
                && activeRouteId.getSidebarItem() == sidebarItem;
    }

    private void showTooltip(Button button) {
        Popup tooltip = this.buttonTooltips.get(button);
        if (tooltip == null)
            return;

        hideAllTooltips();

        var bounds = button.localToScreen(button.getBoundsInLocal());
        if (bounds == null)
            return;

        var root = (Region) tooltip.getContent().getFirst();
        root.applyCss();
        root.autosize();

        double tooltipY = bounds.getMinY() + (bounds.getHeight() - root.getHeight()) / 2.0;
        tooltip.show(button, bounds.getMaxX() + 8, tooltipY);
    }

    private void hideTooltip(Button button) {
        Popup tooltip = this.buttonTooltips.get(button);
        if (tooltip != null) {
            tooltip.hide();
        }
    }

    private void hideAllTooltips() {
        for (Popup tooltip : this.buttonTooltips.values()) {
            tooltip.hide();
        }
    }

    private Popup createSidebarTooltip(String text) {
        var label = new Label(text);
        label.getStyleClass().add("dashboard-nav-tooltip-label");

        var bubble = new StackPane(label);
        bubble.getStyleClass().add("dashboard-nav-tooltip-bubble");

        var arrow = new StackPane();
        arrow.getStyleClass().add("dashboard-nav-tooltip-arrow");
        arrow.setPrefSize(12, 12);
        arrow.setMinSize(12, 12);
        arrow.setMaxSize(12, 12);
        arrow.setRotate(45);

        var root = new HBox(-4, arrow, bubble);
        root.getStyleClass().add("dashboard-nav-tooltip-root");
        root.setAlignment(Pos.CENTER_LEFT);
        root.setMouseTransparent(true);
        root.getStylesheets().add(Stylesheets.resolve("dashboard-shell.css"));

        var popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(false);
        popup.getContent().add(root);
        return popup;
    }

    private Separator createSidebarSeparator() {
        var separator = new Separator();
        separator.getStyleClass().add("dashboard-sidebar-separator");
        return separator;
    }

    private void signOut() {
        Scene scene = getScene();
        if (scene != null) {
            scene.setRoot(new PickAccountPane());
        }
    }
}
