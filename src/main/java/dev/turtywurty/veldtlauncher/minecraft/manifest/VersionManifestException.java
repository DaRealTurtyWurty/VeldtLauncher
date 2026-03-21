package dev.turtywurty.veldtlauncher.minecraft.manifest;

public class VersionManifestException extends RuntimeException {
    public VersionManifestException(String message) {
        super(message);
    }

    public VersionManifestException(String message, Throwable cause) {
        super(message, cause);
    }

    public VersionManifestException(Throwable cause) {
        super(cause);
    }
}
