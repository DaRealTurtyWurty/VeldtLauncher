package dev.turtywurty.veldtlauncher.auth.session;

import java.util.List;
import java.util.Optional;

public interface SessionStore {
    List<StoredSessionMetadata> loadAll();

    default Optional<StoredSessionMetadata> load() {
        return loadLastSession().or(() -> {
            List<StoredSessionMetadata> sessions = loadAll();
            if (sessions.isEmpty())
                return Optional.empty();

            return Optional.of(sessions.getLast());
        });
    }

    default Optional<StoredSessionMetadata> load(String userId) {
        if (userId == null || userId.isBlank())
            return Optional.empty();

        return loadAll().stream()
                .filter(session -> userId.equals(session.userId()))
                .findFirst();
    }

    Optional<StoredSessionMetadata> loadLastSession();

    void save(StoredSessionMetadata session);

    void setLastSession(String userId);

    void delete(String userId);

    void clear();
}
