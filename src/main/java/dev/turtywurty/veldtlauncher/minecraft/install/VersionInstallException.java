package dev.turtywurty.veldtlauncher.minecraft.install;

public class VersionInstallException extends RuntimeException {
    public VersionInstallException(String message) {
        super(message);
    }

    public VersionInstallException(String message, Throwable cause) {
        super(message, cause);
    }

    public VersionInstallException(Throwable cause) {
        super(cause);
    }
}
