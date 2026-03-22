package dev.turtywurty.veldtlauncher.config;

import dev.turtywurty.veldtlauncher.util.OperatingSystem;

import java.nio.file.Path;

public final class FileConfig {
    public static final String DEFAULT_SESSION_FILE = "session.json";
    public static final String DEFAULT_GAME_DIRECTORY = "game";

    private FileConfig() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String getSessionFile() {
        return System.getenv().getOrDefault("VELDT_SESSION_FILE", DEFAULT_SESSION_FILE);
    }

    public static String getGameDirectory() {
        return System.getenv().getOrDefault("VELDT_GAME_DIRECTORY", DEFAULT_GAME_DIRECTORY);
    }

    public static Path resolveConfigFile(String filename) {
        return OperatingSystem.getConfigDirectory().resolve(filename);
    }
}
