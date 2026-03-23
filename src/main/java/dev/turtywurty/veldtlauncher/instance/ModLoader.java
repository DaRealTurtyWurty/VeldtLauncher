package dev.turtywurty.veldtlauncher.instance;

public enum ModLoader {
    FORGE("Forge"),
    FABRIC("Fabric"),
    NEOFORGE("NeoForge"),
    QUILT("Quilt");

    private final String displayName;

    ModLoader(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
