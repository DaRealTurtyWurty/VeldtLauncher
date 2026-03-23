package dev.turtywurty.veldtlauncher.ui.dashboard.page.library;

import dev.turtywurty.veldtlauncher.ui.dashboard.navigation.Navigator;
import dev.turtywurty.veldtlauncher.ui.dashboard.shell.DashboardShell;
import javafx.scene.control.Tooltip;

import java.util.function.Consumer;

public interface LibraryItemContent {
    String getName();

    String getIconPath();

    String getDescription();

    Consumer<Navigator> getOnClickAction();

    default Consumer<Navigator> getOnPlayAction() {
        return getOnClickAction();
    }

    default void onPlay(Navigator navigator, DashboardShell shell) {
        getOnPlayAction().accept(navigator);
    }

    default Tooltip getTooltip() {
        return new Tooltip(getDescription());
    }

    default String getSearchableText() {
        return getName() + " " + getDescription();
    }

    default boolean matchesSearch(String query) {
        return getSearchableText().toLowerCase().contains(query.toLowerCase());
    }
}
