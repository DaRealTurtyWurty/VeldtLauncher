package dev.turtywurty.veldtlauncher.auth.session.secret;

import java.util.Optional;

public interface SecretStore {
    Optional<String> load(String key);

    void save(String key, String secret);

    void delete(String key);
}
