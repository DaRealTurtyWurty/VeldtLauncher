package dev.turtywurty.veldtlauncher.auth.pkce.state;

import java.security.SecureRandom;
import java.util.Base64;

public class StateFactory implements StateProvider {
    private final SecureRandom secureRandom;

    public StateFactory(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public StateFactory() {
        this(new SecureRandom());
    }

    public String create() {
        byte[] bytes = new byte[16];
        this.secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
