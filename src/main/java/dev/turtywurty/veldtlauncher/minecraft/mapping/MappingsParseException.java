package dev.turtywurty.veldtlauncher.minecraft.mapping;

public class MappingsParseException extends RuntimeException {
    public MappingsParseException(String message) {
        super(message);
    }

    public MappingsParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public MappingsParseException(Throwable cause) {
        super(cause);
    }
}
