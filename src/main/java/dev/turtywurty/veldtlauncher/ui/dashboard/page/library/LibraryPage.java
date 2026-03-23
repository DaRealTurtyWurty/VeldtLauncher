package dev.turtywurty.veldtlauncher.ui.dashboard.page.library;

import dev.turtywurty.veldtlauncher.instance.InstanceType;
import dev.turtywurty.veldtlauncher.instance.JsonInstanceStore;
import dev.turtywurty.veldtlauncher.instance.StoredInstanceMetadata;
import dev.turtywurty.veldtlauncher.ui.Stylesheets;
import dev.turtywurty.veldtlauncher.ui.dashboard.navigation.Navigator;
import dev.turtywurty.veldtlauncher.ui.dashboard.page.VeldtPage;
import dev.turtywurty.veldtlauncher.ui.dashboard.route.RouteId;
import dev.turtywurty.veldtlauncher.ui.dashboard.shell.DashboardShell;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class LibraryPage extends VeldtPage {
    private final RouteId routeId;

    private final ObservableList<LibraryItemContent> contentItems = FXCollections.observableArrayList();

    private final ToggleGroup pageToggles = new ToggleGroup();
    private final LibraryPlaceholder placeholder = new LibraryPlaceholder(this::openAddInstanceOverlay);
    private final FlowPane content = new FlowPane();
    private final HBox toggleBar = new HBox(8);
    private final StackPane contentContainer = new StackPane();
    private final VBox layout = new VBox(16);

    public LibraryPage(RouteId routeId) {
        this.routeId = Objects.requireNonNull(routeId, "routeId");
        Stylesheets.add(this, "library-page.css");
        getStyleClass().add("library-page");

        configureToggleBar();
        configureContent();

        this.contentItems.addListener((ListChangeListener<LibraryItemContent>) _ -> refreshContent());
        reload();
    }

    @Override
    public String getTitle() {
        return "Library";
    }

    @Override
    public RouteId getRouteId() {
        return this.routeId;
    }

    public void reload() {
        loadContentItems();
        refreshContent();
    }

    private void configureToggleBar() {
        this.toggleBar.getStyleClass().add("library-page-toggle-group");
        this.toggleBar.setAlignment(Pos.CENTER_LEFT);

        this.pageToggles.selectedToggleProperty().addListener((_, _, selectedToggle) -> handleToggleSelection(selectedToggle));

        ToggleButton allInstancesButton = createToggle("All Instances", RouteId.LIBRARY_ALL);
        ToggleButton modpacksButton = createToggle("Modpacks", RouteId.LIBRARY_MODPACKS);
        ToggleButton serversButton = createToggle("Servers", RouteId.LIBRARY_SERVERS);

        this.toggleBar.getChildren().setAll(allInstancesButton, modpacksButton, serversButton);
        selectCurrentToggle();
    }

    private void configureContent() {
        this.content.getStyleClass().add("library-page-content");
        this.content.setHgap(10);
        this.content.setVgap(10);
        this.content.prefWrapLengthProperty().bind(this.contentContainer.widthProperty());

        this.contentContainer.getStyleClass().add("library-page-content-container");
        VBox.setVgrow(this.contentContainer, Priority.ALWAYS);

        this.layout.getStyleClass().add("library-page-layout");
        this.layout.getChildren().setAll(this.toggleBar, this.contentContainer);

        getChildren().setAll(this.layout);
    }

    private ToggleButton createToggle(String label, RouteId targetRouteId) {
        var button = new ToggleButton(label);
        button.getStyleClass().add("library-page-toggle");
        button.setMnemonicParsing(false);
        button.setToggleGroup(this.pageToggles);
        button.setUserData(targetRouteId);
        return button;
    }

    private void handleToggleSelection(Toggle selectedToggle) {
        if (selectedToggle == null) {
            selectCurrentToggle();
            return;
        }

        RouteId targetRouteId = (RouteId) selectedToggle.getUserData();
        if (targetRouteId == this.routeId)
            return;

        Navigator navigator = resolveNavigator();
        if (navigator == null) {
            selectCurrentToggle();
            return;
        }

        navigator.navigateTo(targetRouteId);
    }

    private void refreshContent() {
        if (this.contentItems.isEmpty()) {
            StackPane.setAlignment(this.placeholder, Pos.CENTER);
            this.contentContainer.getChildren().setAll(this.placeholder);
            return;
        }

        this.content.getChildren().clear();
        for (LibraryItemContent item : this.contentItems) {
            this.content.getChildren().add(new LibraryItem<>(item));
        }

        StackPane.setAlignment(this.content, Pos.TOP_LEFT);
        this.contentContainer.getChildren().setAll(this.content);
    }

    private void loadContentItems() {
        List<LibraryItemContent> items = JsonInstanceStore.INSTANCE.loadAll().stream()
                .filter(this::matchesCurrentRoute)
                .sorted(compareInstances())
                .map(StoredInstanceLibraryItemContent::new)
                .map(LibraryItemContent.class::cast)
                .toList();
        this.contentItems.setAll(items);
    }

    private boolean matchesCurrentRoute(StoredInstanceMetadata instance) {
        return switch (this.routeId) {
            case LIBRARY_MODPACKS -> instance.type() == InstanceType.MODPACK;
            case LIBRARY_SERVERS -> instance.type() == InstanceType.SERVER;
            case LIBRARY_ALL -> true;
            default -> false;
        };
    }

    private Comparator<StoredInstanceMetadata> compareInstances() {
        return Comparator
                .comparing(StoredInstanceMetadata::lastPlayedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(StoredInstanceMetadata::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(instance -> {
                    String name = instance.name();
                    return name == null ? "" : name.toLowerCase();
                });
    }

    private void selectCurrentToggle() {
        for (Toggle toggle : this.pageToggles.getToggles()) {
            if (toggle.getUserData() == this.routeId) {
                this.pageToggles.selectToggle(toggle);
                return;
            }
        }
    }

    private Navigator resolveNavigator() {
        Parent parent = getParent();
        while (parent != null) {
            if (parent instanceof DashboardShell shell)
                return shell.getNavigator();

            parent = parent.getParent();
        }

        if (getScene() != null && getScene().getRoot() instanceof DashboardShell shell)
            return shell.getNavigator();

        return null;
    }

    private void openAddInstanceOverlay() {
        Parent parent = getParent();
        while (parent != null) {
            if (parent instanceof DashboardShell shell) {
                shell.showAddInstanceOverlay();
                return;
            }

            parent = parent.getParent();
        }

        if (getScene() != null && getScene().getRoot() instanceof DashboardShell shell) {
            shell.showAddInstanceOverlay();
        }
    }
}
