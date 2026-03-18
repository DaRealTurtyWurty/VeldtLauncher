package dev.turtywurty.veldtlauncher.auth.session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import dev.turtywurty.veldtlauncher.config.FileConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class JsonSessionStore implements SessionStore {
    public static final JsonSessionStore INSTANCE =
            new JsonSessionStore(FileConfig.resolveConfigFile(FileConfig.getSessionFile()));

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonSessionStore.class);
    private static final Type SESSION_LIST_TYPE = new TypeToken<List<StoredSessionMetadata>>() { }.getType();

    private final Path path;

    public JsonSessionStore(Path path) {
        this.path = path;
    }

    @Override
    public synchronized List<StoredSessionMetadata> loadAll() {
        return loadState().sessions();
    }

    @Override
    public synchronized Optional<StoredSessionMetadata> loadLastSession() {
        SessionStoreState state = loadState();
        if (state.lastSessionUserId() != null && !state.lastSessionUserId().isBlank()) {
            for (StoredSessionMetadata session : state.sessions()) {
                if (state.lastSessionUserId().equals(session.userId()))
                    return Optional.of(session);
            }
        }

        if (state.sessions().isEmpty())
            return Optional.empty();

        return Optional.of(state.sessions().getLast());
    }

    @Override
    public synchronized void save(StoredSessionMetadata session) {
        if (session == null)
            return;

        try {
            SessionStoreState state = loadState();
            List<StoredSessionMetadata> sessions = new ArrayList<>(state.sessions());
            sessions.removeIf(existing -> hasSameUser(existing, session));
            sessions.add(session);
            writeState(new SessionStoreState(sessions, session.userId()));
        } catch (Exception exception) {
            LOGGER.error("Failed to save StoredSessionMetadata entry to file: {}", this.path, exception);
        }
    }

    @Override
    public synchronized void setLastSession(String userId) {
        if (userId == null || userId.isBlank())
            return;

        try {
            SessionStoreState state = loadState();
            long now = System.currentTimeMillis();
            List<StoredSessionMetadata> updatedSessions = state.sessions().stream()
                    .map(session -> userId.equals(session.userId()) ? touch(session, now) : session)
                    .toList();
            boolean exists = updatedSessions.stream()
                    .anyMatch(session -> userId.equals(session.userId()));
            if (!exists)
                return;

            writeState(new SessionStoreState(updatedSessions, userId));
        } catch (Exception exception) {
            LOGGER.error("Failed to update last StoredSessionMetadata entry in file: {}", this.path, exception);
        }
    }

    @Override
    public synchronized void delete(String userId) {
        if (userId == null || userId.isBlank())
            return;

        try {
            SessionStoreState state = loadState();
            List<StoredSessionMetadata> sessions = new ArrayList<>(state.sessions());
            sessions.removeIf(session -> userId.equals(session.userId()));
            String lastSessionUserId = state.lastSessionUserId();
            if (Objects.equals(lastSessionUserId, userId) || !containsUser(sessions, lastSessionUserId)) {
                lastSessionUserId = sessions.isEmpty() ? null : sessions.getLast().userId();
            }

            writeState(new SessionStoreState(sessions, lastSessionUserId));
        } catch (Exception exception) {
            LOGGER.error("Failed to delete StoredSessionMetadata entry from file: {}", this.path, exception);
        }
    }

    @Override
    public synchronized void clear() {
        try {
            Files.deleteIfExists(this.path);
        } catch (Exception exception) {
            LOGGER.error("Failed to clear StoredSessionMetadata file: {}", this.path, exception);
        }
    }

    private SessionStoreState loadState() {
        try {
            createParentDirectories();
            if (Files.notExists(this.path))
                return SessionStoreState.empty();

            String content = Files.readString(this.path);
            if (content.isBlank())
                return SessionStoreState.empty();

            JsonElement jsonElement = JsonParser.parseString(content);
            if (jsonElement.isJsonArray()) {
                List<StoredSessionMetadata> sessions = GSON.fromJson(jsonElement, SESSION_LIST_TYPE);
                List<StoredSessionMetadata> sanitized = sanitizeSessions(sessions);
                return new SessionStoreState(sanitized, sanitized.isEmpty() ? null : sanitized.getLast().userId());
            }

            if (jsonElement.isJsonObject()) {
                if (jsonElement.getAsJsonObject().has("sessions") || jsonElement.getAsJsonObject().has("lastSessionUserId")) {
                    SessionStoreState state = GSON.fromJson(jsonElement, SessionStoreState.class);
                    if (state == null)
                        return SessionStoreState.empty();

                    List<StoredSessionMetadata> sessions = sanitizeSessions(state.sessions());
                    String lastSessionUserId = normalizeLastSessionUserId(sessions, state.lastSessionUserId());
                    return new SessionStoreState(sessions, lastSessionUserId);
                }

                StoredSessionMetadata session = GSON.fromJson(jsonElement, StoredSessionMetadata.class);
                if (session == null)
                    return SessionStoreState.empty();

                return new SessionStoreState(List.of(session), session.userId());
            }

            LOGGER.warn("Unexpected session store format in file: {}", this.path);
            return SessionStoreState.empty();
        } catch (Exception exception) {
            LOGGER.error("Failed to load StoredSessionMetadata entries from file: {}", this.path, exception);
            return SessionStoreState.empty();
        }
    }

    private void createParentDirectories() throws Exception {
        Path parent = this.path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private void writeState(SessionStoreState state) throws Exception {
        createParentDirectories();
        List<StoredSessionMetadata> sessions = sanitizeSessions(state.sessions());
        String lastSessionUserId = normalizeLastSessionUserId(sessions, state.lastSessionUserId());
        String content = GSON.toJson(new SessionStoreState(sessions, lastSessionUserId));
        Files.writeString(this.path, content);
    }

    private List<StoredSessionMetadata> sanitizeSessions(List<StoredSessionMetadata> sessions) {
        if (sessions == null || sessions.isEmpty())
            return List.of();

        return sessions.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean hasSameUser(StoredSessionMetadata left, StoredSessionMetadata right) {
        return left != null
                && right != null
                && Objects.equals(left.userId(), right.userId());
    }

    private StoredSessionMetadata touch(StoredSessionMetadata session, long timestamp) {
        if (session == null)
            return null;

        return new StoredSessionMetadata(
                session.userId(),
                session.username(),
                session.expiresAt(),
                session.accountId(),
                timestamp
        );
    }

    private boolean containsUser(List<StoredSessionMetadata> sessions, String userId) {
        if (userId == null || userId.isBlank() || sessions == null || sessions.isEmpty())
            return false;

        return sessions.stream().anyMatch(session -> userId.equals(session.userId()));
    }

    private String normalizeLastSessionUserId(List<StoredSessionMetadata> sessions, String userId) {
        if (containsUser(sessions, userId))
            return userId;

        return sessions == null || sessions.isEmpty() ? null : sessions.getLast().userId();
    }

    private record SessionStoreState(List<StoredSessionMetadata> sessions, String lastSessionUserId) {
        private static SessionStoreState empty() {
            return new SessionStoreState(List.of(), null);
        }
    }
}
