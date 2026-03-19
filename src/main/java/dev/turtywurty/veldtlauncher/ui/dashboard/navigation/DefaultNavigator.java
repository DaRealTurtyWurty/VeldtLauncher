package dev.turtywurty.veldtlauncher.ui.dashboard.navigation;

import dev.turtywurty.veldtlauncher.ui.dashboard.route.RouteId;
import dev.turtywurty.veldtlauncher.ui.dashboard.route.RouteRegistry;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DefaultNavigator implements Navigator {
    private final RouteRegistry routeRegistry;
    private final Deque<RouteId> previousPages = new ArrayDeque<>();
    private final Deque<RouteId> nextPages = new ArrayDeque<>();
    private final List<NavigationListener> listeners = new CopyOnWriteArrayList<>();

    private RouteId currentPage;

    public DefaultNavigator() {
        this(RouteRegistry.INSTANCE, RouteId.HOME);
    }

    public DefaultNavigator(RouteId initialRouteId) {
        this(RouteRegistry.INSTANCE, initialRouteId);
    }

    public DefaultNavigator(RouteRegistry routeRegistry, RouteId initialRouteId) {
        this.routeRegistry = Objects.requireNonNull(routeRegistry, "routeRegistry");
        this.currentPage = requireRegisteredRoute(initialRouteId);
    }

    @Override
    public void navigateTo(RouteId routeId) {
        RouteId nextRouteId = requireRegisteredRoute(routeId);
        if (nextRouteId == this.currentPage)
            return;

        RouteId oldRouteId = this.currentPage;
        this.previousPages.push(oldRouteId);
        this.nextPages.clear();
        this.currentPage = nextRouteId;
        notifyListeners(oldRouteId, nextRouteId, NavigationDirection.NEW);
    }

    @Override
    public RouteId navigateBack() {
        if (!canNavigateBack())
            return this.currentPage;

        RouteId oldRouteId = this.currentPage;
        this.nextPages.push(oldRouteId);
        this.currentPage = this.previousPages.pop();
        notifyListeners(oldRouteId, this.currentPage, NavigationDirection.BACKWARD);
        return this.currentPage;
    }

    @Override
    public RouteId navigateForward() {
        if (!canNavigateForward())
            return this.currentPage;

        RouteId oldRouteId = this.currentPage;
        this.previousPages.push(oldRouteId);
        this.currentPage = this.nextPages.pop();
        notifyListeners(oldRouteId, this.currentPage, NavigationDirection.FORWARD);
        return this.currentPage;
    }

    @Override
    public boolean canNavigateBack() {
        return !this.previousPages.isEmpty();
    }

    @Override
    public boolean canNavigateForward() {
        return !this.nextPages.isEmpty();
    }

    @Override
    public RouteId getCurrentPage() {
        return this.currentPage;
    }

    @Override
    public List<RouteId> getPreviousPages() {
        return List.copyOf(this.previousPages);
    }

    @Override
    public List<RouteId> getNextPages() {
        return List.copyOf(this.nextPages);
    }

    @Override
    public void addNavigationListener(NavigationListener listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void removeNavigationListener(NavigationListener listener) {
        this.listeners.remove(listener);
    }

    private RouteId requireRegisteredRoute(RouteId routeId) {
        RouteId value = Objects.requireNonNull(routeId, "routeId");
        if (!this.routeRegistry.hasRoute(value))
            throw new IllegalArgumentException("No route registered for id: " + value);

        return value;
    }

    private void notifyListeners(RouteId oldRouteId, RouteId newRouteId, NavigationDirection direction) {
        for (NavigationListener listener : this.listeners) {
            listener.onNavigate(oldRouteId, newRouteId, direction);
        }
    }
}
