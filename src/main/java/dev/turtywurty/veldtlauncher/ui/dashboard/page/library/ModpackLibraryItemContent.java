package dev.turtywurty.veldtlauncher.ui.dashboard.page.library;

import dev.turtywurty.veldtlauncher.ui.dashboard.navigation.Navigator;
import dev.turtywurty.veldtlauncher.ui.dashboard.route.RouteId;

import java.util.function.Consumer;

public class ModpackLibraryItemContent implements LibraryItemContent {
    private final String name;
    private final String version;
    private final String mcVersion;
    private final String loader;
    private final String iconUrl;

    public ModpackLibraryItemContent(String name, String version, String mcVersion, String loader, String iconUrl) {
        this.name = name;
        this.version = version;
        this.mcVersion = mcVersion;
        this.loader = loader;
        this.iconUrl = iconUrl;
    }

    @Override
    public String getName() {
        return this.name + " " + this.version;
    }

    @Override
    public String getIconPath() {
        return this.iconUrl;
    }

    @Override
    public String getDescription() {
        return String.format("%s %s", this.loader, this.mcVersion);
    }

    @Override
    public String getSearchableText() {
        return String.format("%s %s %s %s", this.name, this.version, this.mcVersion, this.loader);
    }

    @Override
    public Consumer<Navigator> getOnClickAction() {
        return navigator -> navigator.navigateTo(RouteId.VIEW_INSTANCE);
    }

    @Override
    public Consumer<Navigator> getOnPlayAction() {
        return navigator -> navigator.navigateTo(RouteId.VIEW_INSTANCE);
    }
}
