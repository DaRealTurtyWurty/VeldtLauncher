package dev.turtywurty.veldtlauncher.minecraft.metadata.model;

public record VersionRange(
        String min,
        String max
) {
    public boolean includes(String version) {
        if (version == null || version.isEmpty())
            return false;

        if (min != null && !(version.compareTo(min) >= 0))
            return false;

        if (max != null && !(version.compareTo(max) <= 0))
            return false;

        return true;
    }
}
