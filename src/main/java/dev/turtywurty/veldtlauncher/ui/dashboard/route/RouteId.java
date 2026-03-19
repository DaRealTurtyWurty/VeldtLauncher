package dev.turtywurty.veldtlauncher.ui.dashboard.route;

import dev.turtywurty.veldtlauncher.ui.dashboard.shell.SidebarItem;

public enum RouteId {
    HOME(SidebarItem.LIBRARY),
    LIBRARY_ALL(SidebarItem.LIBRARY),
    LIBRARY_MODPACKS(SidebarItem.LIBRARY),
    LIBRARY_SERVERS(SidebarItem.LIBRARY),
    SETTINGS(SidebarItem.SETTINGS),
    ADD_INSTANCE(SidebarItem.ADD_INSTANCE),
    EDIT_INSTANCE(SidebarItem.ADD_INSTANCE),
    VIEW_INSTANCE(SidebarItem.ADD_INSTANCE),
    DISCOVER_MODPACKS(SidebarItem.DISCOVER),
    DISCOVER_MODS(SidebarItem.DISCOVER),
    DISCOVER_RESOURCE_PACKS(SidebarItem.DISCOVER),
    DISCOVER_SHADER_PACKS(SidebarItem.DISCOVER),
    DISCOVER_DATAPACKS(SidebarItem.DISCOVER),
    DISCOVER_SERVERS(SidebarItem.DISCOVER),
    VIEW_MODPACK(SidebarItem.DISCOVER),
    VIEW_MOD(SidebarItem.DISCOVER),
    VIEW_RESOURCE_PACK(SidebarItem.DISCOVER),
    VIEW_SHADER_PACK(SidebarItem.DISCOVER),
    VIEW_DATAPACK(SidebarItem.DISCOVER),
    VIEW_SERVER(SidebarItem.DISCOVER),
    VIEW_PROFILE(SidebarItem.PROFILE);

    private final SidebarItem sidebarItem;

    RouteId(SidebarItem sidebarItem) {
        this.sidebarItem = sidebarItem;
    }

    public SidebarItem getSidebarItem() {
        return this.sidebarItem;
    }
}
