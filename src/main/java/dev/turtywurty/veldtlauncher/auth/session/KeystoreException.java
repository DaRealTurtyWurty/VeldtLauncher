package dev.turtywurty.veldtlauncher.auth.session;

public class KeystoreException extends RuntimeException {
    public KeystoreException(String message) {
        super(message);
    }

    public KeystoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeystoreException(Throwable cause) {
        super(cause);
    }
}
