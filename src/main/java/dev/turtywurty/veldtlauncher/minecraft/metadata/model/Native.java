package dev.turtywurty.veldtlauncher.minecraft.metadata.model;

import dev.turtywurty.veldtlauncher.util.OperatingSystem;

public record Native(
        String linux,
        String macos,
        String windows,
        String osx
) {
    public String classifierForCurrentEnvironment() {
        String classifier = switch (OperatingSystem.CURRENT) {
            case WINDOWS -> windows;
            case MAC -> macos != null && !macos.isBlank() ? macos : osx;
            case LINUX -> linux;
            case UNKNOWN -> null;
        };

        if (classifier == null || classifier.isBlank())
            return null;

        return classifier.replace("${arch}", is64BitArchitecture() ? "64" : "32");
    }

    private static boolean is64BitArchitecture() {
        String architecture = System.getProperty("os.arch", "").toLowerCase();
        return architecture.contains("64")
                || architecture.contains("arm64")
                || architecture.contains("aarch64");
    }
}
