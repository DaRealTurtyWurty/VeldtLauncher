package dev.turtywurty.veldtlauncher.minecraft.java;

public class JavaDiscoveryException extends RuntimeException {
    public JavaDiscoveryException(String message) {
        super(message);
    }

    public JavaDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public JavaDiscoveryException(Throwable cause) {
        super(cause);
    }
}
