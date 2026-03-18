package dev.turtywurty.veldtlauncher.config;

import dev.turtywurty.veldtlauncher.util.OperatingSystem;

import java.nio.file.Path;

public final class FileConfig {
    public static final String DEFAULT_SESSION_FILE = "session.json";

    private FileConfig() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String getSessionFile() {
        return System.getenv().getOrDefault("VELDT_SESSION_FILE", DEFAULT_SESSION_FILE);
    }

    public static Path resolveConfigFile(String filename) {
        return OperatingSystem.getConfigDirectory().resolve(filename);
    }
}
