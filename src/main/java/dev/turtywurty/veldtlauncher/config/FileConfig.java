package dev.turtywurty.veldtlauncher.config;

import dev.turtywurty.veldtlauncher.util.OperatingSystem;

import java.nio.file.Path;

public final class FileConfig {
    public static final String DEFAULT_SESSION_FILE = "session.json";
    public static final String DEFAULT_INSTANCE_FILE = "instances.json";
    public static final String DEFAULT_INSTANCES_DIRECTORY = "instances";
    public static final String DEFAULT_GAME_DIRECTORY = "game";

    private FileConfig() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String getSessionFile() {
        return System.getenv().getOrDefault("VELDT_SESSION_FILE", DEFAULT_SESSION_FILE);
    }

    public static String getInstanceFile() {
        return System.getenv().getOrDefault("VELDT_INSTANCE_FILE", DEFAULT_INSTANCE_FILE);
    }

    public static String getInstancesDirectory() {
        return System.getenv().getOrDefault("VELDT_INSTANCES_DIRECTORY", DEFAULT_INSTANCES_DIRECTORY);
    }

    public static String getGameDirectory() {
        return System.getenv().getOrDefault("VELDT_GAME_DIRECTORY", DEFAULT_GAME_DIRECTORY);
    }

    public static Path resolveConfigFile(String filename) {
        return OperatingSystem.getConfigDirectory().resolve(filename);
    }

    public static Path resolveInstancesDirectory() {
        return OperatingSystem.getConfigDirectory().resolve(getInstancesDirectory());
    }

    public static Path resolveInstanceDirectory(String instanceId) {
        return resolveInstancesDirectory().resolve(instanceId);
    }
}
