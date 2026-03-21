package dev.turtywurty.veldtlauncher.minecraft.metadata.model;

public record Rule(
        String action,
        Os os
) {
    public boolean appliesToCurrentEnvironment() {
        if (os == null)
            return "allow".equals(action);

        String name = os.name();
        String arch = os.arch();
        VersionRange versionRange = os.versionRange();

        String currentOs = System.getProperty("os.name").toLowerCase();
        String currentArch = System.getProperty("os.arch").toLowerCase();
        String currentVersion = System.getProperty("os.version").toLowerCase();

        boolean osMatches = name == null || currentOs.contains(name.toLowerCase());
        boolean archMatches = arch == null || currentArch.contains(arch.toLowerCase());
        boolean versionMatches = versionRange == null || versionRange.includes(currentVersion);

        boolean allowed = "allow".equals(action) && osMatches && archMatches && versionMatches;
        boolean disallowed = "disallow".equals(action) && !(osMatches && archMatches && versionMatches);

        return allowed || disallowed;
    }
}
