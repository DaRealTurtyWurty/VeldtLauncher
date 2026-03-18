package dev.turtywurty.veldtlauncher.util;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Enum representing the operating system on which the application is running.
 * It provides methods to detect the current operating system and retrieve it.
 */
public enum OperatingSystem {
    WINDOWS, MAC, LINUX, UNKNOWN;

    /**
     * The current operating system detected at runtime.
     * This is a static final field that is initialised when the class is loaded.
     */
    @NotNull
    public static final OperatingSystem CURRENT = detect();

    /**
     * Detects the operating system based on the system property "os.name".
     * It checks for known substrings to determine if the OS is Windows, Mac, Linux, or unknown.
     *
     * @return the detected OperatingSystem enum value
     */
    public static @NotNull OperatingSystem detect() {
        String os = System.getProperty("os.name", "unknown").toLowerCase(Locale.ENGLISH);
        if (os.contains("win"))
            return WINDOWS;

        if (os.contains("mac"))
            return MAC;

        if (os.contains("nux")
                || os.contains("nix")
                || os.contains("aix"))
            return LINUX;

        return UNKNOWN;
    }

    public static boolean isMac() {
        return CURRENT == MAC;
    }

    public static boolean isWindows() {
        return CURRENT == WINDOWS;
    }

    public static boolean isLinux() {
        return CURRENT == LINUX;
    }

    public static Path getAppDataDirectory() {
        return switch (CURRENT) {
            case WINDOWS -> Path.of(System.getenv("APPDATA"));
            case MAC -> Path.of(System.getProperty("user.home"), "Library", "Application Support");
            case LINUX -> Path.of(System.getProperty("user.home"), ".local", "share");
            default -> Path.of(System.getProperty("user.home"));
        };
    }

    public static Path getConfigDirectory() {
        String configDir = System.getenv("VELDT_CONFIG_DIR");
        if (configDir != null && !configDir.isEmpty()) {
            return Path.of(configDir);
        } else {
            return getAppDataDirectory().resolve("VeldtLauncher");
        }
    }

    public static Path getCacheDirectory() {
        String cacheDir = System.getenv("VELDT_CACHE_DIR");
        if (cacheDir != null && !cacheDir.isEmpty()) {
            return Path.of(cacheDir);
        } else {
            return getAppDataDirectory().resolve("VeldtLauncher").resolve("cache");
        }
    }
}