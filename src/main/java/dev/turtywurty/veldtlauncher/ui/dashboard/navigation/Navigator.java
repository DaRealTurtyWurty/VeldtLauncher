package dev.turtywurty.veldtlauncher.ui.dashboard.navigation;

import dev.turtywurty.veldtlauncher.ui.dashboard.route.RouteId;

import java.util.List;

public interface Navigator {
    void navigateTo(RouteId routeId);

    RouteId navigateBack();

    RouteId navigateForward();

    boolean canNavigateBack();

    boolean canNavigateForward();

    RouteId getCurrentPage();

    List<RouteId> getPreviousPages();

    List<RouteId> getNextPages();

    void addNavigationListener(NavigationListener listener);

    void removeNavigationListener(NavigationListener listener);

    interface NavigationListener {
        void onNavigate(RouteId oldRouteId, RouteId newRouteId, NavigationDirection direction);
    }

    enum NavigationDirection {
        BACKWARD,
        FORWARD,
        NEW
    }
}
