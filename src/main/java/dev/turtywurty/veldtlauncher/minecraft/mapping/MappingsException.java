package dev.turtywurty.veldtlauncher.minecraft.mapping;

public class MappingsException extends RuntimeException {
    public MappingsException(String message) {
        super(message);
    }

    public MappingsException(String message, Throwable cause) {
        super(message, cause);
    }

    public MappingsException(Throwable cause) {
        super(cause);
    }
}
