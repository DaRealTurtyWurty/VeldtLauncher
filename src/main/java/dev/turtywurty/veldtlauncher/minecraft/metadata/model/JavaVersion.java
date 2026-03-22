package dev.turtywurty.veldtlauncher.minecraft.metadata.model;

import java.util.Objects;

public record JavaVersion(
        String component,
        int majorVersion
) {
    public int compareTo(JavaVersion minVersion) {
        if (!Objects.equals(component, minVersion.component()))
            throw new IllegalArgumentException("Cannot compare Java versions with different components: " + component + " vs " + minVersion.component());

        return Integer.compare(majorVersion, minVersion.majorVersion());
    }
}
