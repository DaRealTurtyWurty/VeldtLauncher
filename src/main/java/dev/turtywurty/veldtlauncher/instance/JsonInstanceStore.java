package dev.turtywurty.veldtlauncher.instance;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import dev.turtywurty.veldtlauncher.config.FileConfig;
import dev.turtywurty.veldtlauncher.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JsonInstanceStore implements InstanceStore {
    public static final JsonInstanceStore INSTANCE =
            new JsonInstanceStore(FileConfig.resolveConfigFile(FileConfig.getInstanceFile()));
    private static final String TYPE_FIELD = "type";

    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Path.class, (JsonSerializer<Path>) (src, _, _) ->
                    src == null ? null : new JsonPrimitive(src.toString()))
            .registerTypeHierarchyAdapter(Path.class, (JsonDeserializer<Path>) (json, _, _) ->
                    json == null || json.isJsonNull() ? null : Path.of(json.getAsString()))
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, _, _) ->
                    src == null ? null : new JsonPrimitive(src.toString()))
            .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, _, _) ->
                    json == null || json.isJsonNull() ? null : Instant.parse(json.getAsString()))
            .registerTypeAdapter(StoredInstanceMetadata.class, (JsonSerializer<StoredInstanceMetadata>) (src, _, context) ->
                    serializeInstance(src, context))
            .registerTypeAdapter(StoredInstanceMetadata.class, (JsonDeserializer<StoredInstanceMetadata>) (json, _, context) ->
                    deserializeInstance(json, context))
            .setPrettyPrinting()
            .create();
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonInstanceStore.class);
    private static final Type INSTANCE_LIST_TYPE = new TypeToken<List<StoredInstanceMetadata>>() {
    }.getType();

    private final Path path;

    public JsonInstanceStore(Path path) {
        this.path = path;
    }

    @Override
    public synchronized List<StoredInstanceMetadata> loadAll() {
        return loadState().instances();
    }

    @Override
    public synchronized Optional<StoredInstanceMetadata> loadLastPlayed() {
        InstanceStoreState state = loadState();
        if (state.lastPlayedInstanceId() != null && !state.lastPlayedInstanceId().isBlank()) {
            for (StoredInstanceMetadata instance : state.instances()) {
                if (state.lastPlayedInstanceId().equals(instance.id()))
                    return Optional.of(instance);
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean hasLastPlayed() {
        InstanceStoreState state = loadState();
        return state.lastPlayedInstanceId() != null
                && !state.lastPlayedInstanceId().isBlank()
                && state.instances().stream().anyMatch(
                instance -> state.lastPlayedInstanceId().equals(instance.id()));
    }

    @Override
    public synchronized void save(StoredInstanceMetadata instance) {
        if (instance == null)
            return;

        try {
            InstanceStoreState state = loadState();
            List<StoredInstanceMetadata> instances = new ArrayList<>(state.instances());
            instances.removeIf(existing -> hasSameId(existing, instance));
            instances.add(instance);
            String lastPlayedInstanceId = normalizeLastPlayedInstanceId(instances, state.lastPlayedInstanceId());
            if (lastPlayedInstanceId == null)
                lastPlayedInstanceId = instance.id();

            writeState(new InstanceStoreState(instances, lastPlayedInstanceId));
        } catch (Exception exception) {
            LOGGER.error("Failed to save StoredInstanceMetadata entry to file: {}", this.path, exception);
        }
    }

    @Override
    public synchronized void setLastPlayed(String instanceId) {
        if (instanceId == null || instanceId.isBlank())
            return;

        try {
            InstanceStoreState state = loadState();
            long now = System.currentTimeMillis();
            List<StoredInstanceMetadata> updatedInstances = state.instances().stream()
                    .map(instance -> instanceId.equals(instance.id())
                            ? touch(instance, now)
                            : instance)
                    .toList();
            boolean exists = updatedInstances.stream()
                    .anyMatch(instance -> instanceId.equals(instance.id()));
            if (!exists)
                return;

            writeState(new InstanceStoreState(updatedInstances, instanceId));
        } catch (Exception exception) {
            LOGGER.error("Failed to update last played StoredInstanceMetadata entry in file: {}", this.path, exception);
        }
    }

    @Override
    public synchronized void clearLastPlayed() {
        try {
            InstanceStoreState state = loadState();
            writeState(new InstanceStoreState(state.instances(), null));
        } catch (Exception exception) {
            LOGGER.error("Failed to clear last played StoredInstanceMetadata entry in file: {}", this.path, exception);
        }
    }

    @Override
    public synchronized void delete(String instanceId) {
        if (instanceId == null || instanceId.isBlank())
            return;

        try {
            InstanceStoreState state = loadState();
            List<StoredInstanceMetadata> instances = new ArrayList<>(state.instances());
            instances.removeIf(instance -> instanceId.equals(instance.id()));
            String lastPlayedInstanceId = state.lastPlayedInstanceId();
            if (Objects.equals(lastPlayedInstanceId, instanceId) || !containsInstance(instances, lastPlayedInstanceId)) {
                lastPlayedInstanceId = instances.isEmpty() ? null : instances.getLast().id();
            }

            writeState(new InstanceStoreState(instances, lastPlayedInstanceId));
        } catch (Exception exception) {
            LOGGER.error("Failed to delete StoredInstanceMetadata entry from file: {}", this.path, exception);
        }
    }

    @Override
    public synchronized void clear() {
        try {
            Files.deleteIfExists(this.path);
        } catch (Exception exception) {
            LOGGER.error("Failed to clear StoredInstanceMetadata file: {}", this.path, exception);
        }
    }

    private InstanceStoreState loadState() {
        try {
            createParentDirectories();
            if (Files.notExists(this.path))
                return InstanceStoreState.empty();

            String content = Files.readString(this.path);
            if (content.isBlank())
                return InstanceStoreState.empty();

            JsonElement jsonElement = JsonParser.parseString(content);
            if (jsonElement.isJsonArray()) {
                List<StoredInstanceMetadata> instances = GSON.fromJson(jsonElement, INSTANCE_LIST_TYPE);
                List<StoredInstanceMetadata> sanitized = sanitizeInstances(instances);
                return new InstanceStoreState(sanitized, sanitized.isEmpty() ? null : sanitized.getLast().id());
            }

            if (jsonElement.isJsonObject()) {
                var jsonObject = jsonElement.getAsJsonObject();
                if (JsonUtil.contains(jsonObject, "instances") || JsonUtil.contains(jsonObject, "lastPlayedInstanceId")) {
                    InstanceStoreState state = GSON.fromJson(jsonElement, InstanceStoreState.class);
                    if (state == null)
                        return InstanceStoreState.empty();

                    List<StoredInstanceMetadata> instances = sanitizeInstances(state.instances());
                    String lastPlayedInstanceId = normalizeLastPlayedInstanceId(instances, state.lastPlayedInstanceId());
                    return new InstanceStoreState(instances, lastPlayedInstanceId);
                }

                StoredInstanceMetadata instance = GSON.fromJson(jsonElement, StoredInstanceMetadata.class);
                if (instance == null)
                    return InstanceStoreState.empty();

                List<StoredInstanceMetadata> sanitized = sanitizeInstances(List.of(instance));
                return new InstanceStoreState(sanitized, sanitized.isEmpty() ? null : sanitized.getFirst().id());
            }

            LOGGER.warn("Unexpected instance store format in file: {}", this.path);
            return InstanceStoreState.empty();
        } catch (Exception exception) {
            LOGGER.error("Failed to load StoredInstanceMetadata entries from file: {}", this.path, exception);
            return InstanceStoreState.empty();
        }
    }

    private void createParentDirectories() throws Exception {
        Path parent = this.path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private void writeState(InstanceStoreState state) throws Exception {
        createParentDirectories();
        List<StoredInstanceMetadata> instances = sanitizeInstances(state.instances());
        String lastPlayedInstanceId = normalizeLastPlayedInstanceId(instances, state.lastPlayedInstanceId());
        String content = GSON.toJson(new InstanceStoreState(instances, lastPlayedInstanceId));
        Files.writeString(this.path, content);
    }

    private List<StoredInstanceMetadata> sanitizeInstances(List<StoredInstanceMetadata> instances) {
        if (instances == null || instances.isEmpty())
            return List.of();

        List<StoredInstanceMetadata> sanitized = new ArrayList<>();
        for (StoredInstanceMetadata instance : instances) {
            StoredInstanceMetadata normalized = normalize(instance);
            if (normalized == null)
                continue;

            sanitized.removeIf(existing -> Objects.equals(existing.id(), normalized.id()));
            sanitized.add(normalized);
        }

        return List.copyOf(sanitized);
    }

    private StoredInstanceMetadata normalize(StoredInstanceMetadata instance) {
        if (instance == null)
            return null;

        String id = trimToNull(instance.id());
        if (id == null)
            return null;

        return switch (instance) {
            case StoredVanillaInstanceMetadata vanilla -> normalizeVanilla(id, vanilla);
            case StoredModpackInstanceMetadata modpack -> normalizeModpack(id, modpack);
            case StoredServerInstanceMetadata server -> normalizeServer(id, server);
        };
    }

    private boolean hasSameId(StoredInstanceMetadata left, StoredInstanceMetadata right) {
        return left != null
                && right != null
                && Objects.equals(left.id(), right.id());
    }

    private StoredInstanceMetadata touch(StoredInstanceMetadata instance, long timestamp) {
        if (instance == null)
            return null;

        return instance.withLastPlayedAt(Instant.ofEpochMilli(timestamp));
    }

    private boolean containsInstance(List<StoredInstanceMetadata> instances, String instanceId) {
        if (instanceId == null || instanceId.isBlank() || instances == null || instances.isEmpty())
            return false;

        return instances.stream().anyMatch(instance -> instanceId.equals(instance.id()));
    }

    private String normalizeLastPlayedInstanceId(List<StoredInstanceMetadata> instances, String instanceId) {
        if (instanceId == null || instanceId.isBlank())
            return null;

        if (containsInstance(instances, instanceId))
            return instanceId;

        return instances.isEmpty() ? null : instances.getLast().id();
    }

    private String trimToNull(String value) {
        if (value == null)
            return null;

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Path normalizePath(Path path) {
        if (path == null)
            return null;

        String normalized = trimToNull(path.toString());
        return normalized == null ? null : Path.of(normalized);
    }

    private List<String> normalizeIds(List<String> ids) {
        if (ids == null || ids.isEmpty())
            return List.of();

        var normalized = new LinkedHashSet<String>();
        for (String id : ids) {
            String trimmed = trimToNull(id);
            if (trimmed != null)
                normalized.add(trimmed);
        }

        return List.copyOf(normalized);
    }

    private StoredVanillaInstanceMetadata normalizeVanilla(String id, StoredVanillaInstanceMetadata vanilla) {
        String name = trimToNull(vanilla.name());
        String minecraftVersion = trimToNull(vanilla.minecraftVersion());
        Path gameDirectory = normalizePath(vanilla.gameDirectory());
        String iconPath = trimToNull(vanilla.iconPath());
        Instant createdAt = vanilla.createdAt() == null ? Instant.now() : vanilla.createdAt();

        return new StoredVanillaInstanceMetadata(
                id,
                name == null ? defaultName(id, minecraftVersion, null) : name,
                minecraftVersion,
                gameDirectory,
                vanilla.managed(),
                iconPath,
                createdAt,
                vanilla.lastPlayedAt()
        );
    }

    private StoredModpackInstanceMetadata normalizeModpack(String id, StoredModpackInstanceMetadata modpack) {
        String name = trimToNull(modpack.name());
        String minecraftVersion = trimToNull(modpack.minecraftVersion());
        Path gameDirectory = normalizePath(modpack.gameDirectory());
        String iconPath = trimToNull(modpack.iconPath());
        Instant createdAt = modpack.createdAt() == null ? Instant.now() : modpack.createdAt();

        return new StoredModpackInstanceMetadata(
                id,
                name == null ? defaultName(id, minecraftVersion, modpack.modLoader()) : name,
                minecraftVersion,
                modpack.modLoader(),
                gameDirectory,
                modpack.managed(),
                iconPath,
                createdAt,
                modpack.lastPlayedAt()
        );
    }

    private StoredServerInstanceMetadata normalizeServer(String id, StoredServerInstanceMetadata server) {
        String name = trimToNull(server.name());
        String address = trimToNull(server.address());
        String minecraftVersion = trimToNull(server.minecraftVersion());
        List<String> supportedInstanceIds = normalizeIds(server.supportedInstanceIds());
        Path gameDirectory = normalizePath(server.gameDirectory());
        String iconPath = trimToNull(server.iconPath());
        Instant createdAt = server.createdAt() == null ? Instant.now() : server.createdAt();

        return new StoredServerInstanceMetadata(
                id,
                name == null ? defaultServerName(id, address) : name,
                address,
                minecraftVersion,
                supportedInstanceIds,
                gameDirectory,
                server.managed(),
                iconPath,
                createdAt,
                server.lastPlayedAt()
        );
    }

    private String defaultName(String id, String minecraftVersion, ModLoader modLoader) {
        if (minecraftVersion != null && modLoader != null)
            return modLoader.getDisplayName() + " " + minecraftVersion;

        if (minecraftVersion != null)
            return "Minecraft " + minecraftVersion;

        return id;
    }

    private String defaultServerName(String id, String address) {
        return address == null ? id : address;
    }

    private static JsonElement serializeInstance(StoredInstanceMetadata instance, JsonSerializationContext context) {
        if (instance == null)
            return JsonNull.INSTANCE;

        JsonObject jsonObject = switch (instance) {
            case StoredVanillaInstanceMetadata vanilla ->
                    context.serialize(vanilla, StoredVanillaInstanceMetadata.class).getAsJsonObject();
            case StoredModpackInstanceMetadata modpack ->
                    context.serialize(modpack, StoredModpackInstanceMetadata.class).getAsJsonObject();
            case StoredServerInstanceMetadata server ->
                    context.serialize(server, StoredServerInstanceMetadata.class).getAsJsonObject();
        };
        jsonObject.addProperty(TYPE_FIELD, instance.type().name());
        return jsonObject;
    }

    private static StoredInstanceMetadata deserializeInstance(JsonElement json, JsonDeserializationContext context) {
        if (json == null || json.isJsonNull())
            return null;

        JsonObject jsonObject = json.getAsJsonObject();
        InstanceType type = readInstanceType(jsonObject);
        return switch (type) {
            case VANILLA -> context.deserialize(jsonObject, StoredVanillaInstanceMetadata.class);
            case MODPACK -> context.deserialize(jsonObject, StoredModpackInstanceMetadata.class);
            case SERVER -> context.deserialize(jsonObject, StoredServerInstanceMetadata.class);
        };
    }

    private static InstanceType readInstanceType(JsonObject jsonObject) {
        String rawType = jsonObject.has(TYPE_FIELD) && !jsonObject.get(TYPE_FIELD).isJsonNull()
                ? jsonObject.get(TYPE_FIELD).getAsString()
                : null;
        if (rawType != null && !rawType.isBlank())
            return InstanceType.valueOf(rawType);

        if (jsonObject.has("modLoader"))
            return InstanceType.MODPACK;

        if (jsonObject.has("address"))
            return InstanceType.SERVER;

        String legacyType = jsonObject.has("type") && !jsonObject.get("type").isJsonNull()
                ? jsonObject.get("type").getAsString()
                : null;
        if (legacyType != null && !legacyType.isBlank())
            return InstanceType.valueOf(legacyType);

        return InstanceType.VANILLA;
    }

    private record InstanceStoreState(List<StoredInstanceMetadata> instances, String lastPlayedInstanceId) {
        private static InstanceStoreState empty() {
            return new InstanceStoreState(List.of(), null);
        }
    }
}
