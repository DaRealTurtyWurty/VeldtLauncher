package dev.turtywurty.veldtlauncher.ui.dashboard.route;

import dev.turtywurty.veldtlauncher.ui.dashboard.page.PlaceholderPage;
import dev.turtywurty.veldtlauncher.ui.dashboard.page.VeldtPage;
import dev.turtywurty.veldtlauncher.ui.dashboard.page.library.LibraryPage;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class RouteRegistry {
    public static final RouteRegistry INSTANCE = new RouteRegistry();

    private final Map<RouteId, Supplier<VeldtPage>> routes = new HashMap<>();

    private RouteRegistry() {
        registerRoute(RouteId.LIBRARY_ALL, () -> new LibraryPage(RouteId.LIBRARY_ALL));
        registerRoute(RouteId.LIBRARY_MODPACKS, () -> new LibraryPage(RouteId.LIBRARY_MODPACKS));
        registerRoute(RouteId.LIBRARY_SERVERS, () -> new LibraryPage(RouteId.LIBRARY_SERVERS));
        registerRoute(RouteId.DISCOVER_MODPACKS, () -> new PlaceholderPage(
                RouteId.DISCOVER_MODPACKS,
                "Discover",
                "Browse fresh modpacks, mods, and servers to bring into your launcher."
        ));
        registerRoute(RouteId.ADD_INSTANCE, () -> new PlaceholderPage(
                RouteId.ADD_INSTANCE,
                "Add Instance",
                "Create a new instance, import an existing setup, or start from a clean version profile."
        ));
        registerRoute(RouteId.SETTINGS, () -> new PlaceholderPage(
                RouteId.SETTINGS,
                "Settings",
                "Tune Java, memory, download behavior, and the rest of the launcher defaults."
        ));
        registerRoute(RouteId.VIEW_PROFILE, () -> new PlaceholderPage(
                RouteId.VIEW_PROFILE,
                "Profile",
                "Review the current player profile, account state, and launcher identity details."
        ));
    }

    public void registerRoute(RouteId id, Supplier<VeldtPage> pageSupplier) {
        routes.put(id, pageSupplier);
    }

    public boolean hasRoute(RouteId id) {
        return routes.containsKey(id);
    }

    public VeldtPage getRouteContent(RouteId id) {
        Supplier<VeldtPage> supplier = routes.get(id);
        if (supplier == null)
            throw new IllegalArgumentException("No route registered for id: " + id);

        return supplier.get();
    }
}
