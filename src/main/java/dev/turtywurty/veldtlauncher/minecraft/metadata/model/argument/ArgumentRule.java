package dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument;

import dev.turtywurty.veldtlauncher.minecraft.metadata.model.Os;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.VersionRange;

import java.util.Map;

public record ArgumentRule(
        String action,
        Map<String, Boolean> features,
        Os os
) {
    public boolean appliesToCurrentEnvironment(Map<String, Boolean> currentFeatures) {
        boolean featuresMatch = true;
        if (features != null && !features.isEmpty()) {
            for (Map.Entry<String, Boolean> entry : features.entrySet()) {
                if (!entry.getValue().equals(currentFeatures.get(entry.getKey())))
                    return false;
            }
        }

        if (os == null)
            return featuresMatch;

        String name = os.name();
        String arch = os.arch();
        VersionRange versionRange = os.versionRange();

        String currentOs = System.getProperty("os.name", "").toLowerCase();
        String currentArch = System.getProperty("os.arch", "").toLowerCase();
        String currentVersion = System.getProperty("os.version", "").toLowerCase();

        boolean osMatches = name == null || currentOs.contains(name.toLowerCase());
        boolean archMatches = arch == null || currentArch.contains(arch.toLowerCase());
        boolean versionMatches = versionRange == null || versionRange.includes(currentVersion);
        return osMatches && archMatches && versionMatches;
    }
}
