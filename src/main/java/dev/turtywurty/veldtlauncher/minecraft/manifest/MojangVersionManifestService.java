package dev.turtywurty.veldtlauncher.minecraft.manifest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadataParser;
import dev.turtywurty.veldtlauncher.util.JsonUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class MojangVersionManifestService implements VersionManifestService {
    // TODO: This will be put into settings later, but for now we can just hardcode it here
    private static final boolean USE_OLD_LAUNCHER_META = false;

    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;
    private final VersionManifestCache cache;
    private final Path gameDirectory;

    public MojangVersionManifestService(HttpClient httpClient, VersionManifestCache cache, Path gameDirectory) {
        this.httpClient = httpClient;
        this.cache = cache;
        this.gameDirectory = gameDirectory;
    }

    public MojangVersionManifestService(Path gameDirectory) {
        this(HttpClient.newHttpClient(), new InMemoryVersionManifestCache(), gameDirectory);
    }

    @Override
    public VersionManifest fetchManifest() throws VersionManifestException {
        if (cache.getManifest().isPresent())
            return cache.getManifest().get();

        String url = USE_OLD_LAUNCHER_META
                ? "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
                : "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new VersionManifestException("Failed to fetch version manifest: HTTP " + response.statusCode());

            String responseBody = response.body();
            if (responseBody == null || responseBody.isEmpty())
                throw new VersionManifestException("Failed to fetch version manifest: Empty response body");

            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
            JsonObject latestObj = JsonUtil.getObject(json, "latest", new JsonObject());
            var latest = new LatestVersions(
                    JsonUtil.getString(latestObj, "release", ""),
                    JsonUtil.getString(latestObj, "snapshot", "")
            );

            List<VersionManifestEntry> versions = JsonUtil.streamArray(JsonUtil.getArray(json, "versions"))
                    .map(element -> {
                        JsonObject versionObj = element.getAsJsonObject();
                        return new VersionManifestEntry(
                                JsonUtil.getString(versionObj, "id", ""),
                                JsonUtil.getString(versionObj, "type", ""),
                                URI.create(JsonUtil.getString(versionObj, "url", "")),
                                JsonUtil.getString(versionObj, "sha1", ""),
                                JsonUtil.getInstant(versionObj, "releaseTime"),
                                JsonUtil.getInstant(versionObj, "time"),
                                JsonUtil.getInt(versionObj, "complianceLevel", 0)
                        );
                    })
                    .toList();

            var manifest = new VersionManifest(latest, versions);
            cache.putManifest(manifest);
            return manifest;
        } catch (Exception exception) {
            throw new VersionManifestException("Failed to fetch version manifest", exception);
        }
    }

    @Override
    public List<VersionManifestEntry> fetchVersions() throws VersionManifestException {
        return fetchManifest().versions();
    }

    @Override
    public Optional<VersionManifestEntry> fetchVersion(String versionId) throws VersionManifestException {
        return fetchVersions().stream()
                .filter(entry -> entry.id().equals(versionId))
                .findFirst();
    }

    @Override
    public Optional<VersionManifestEntry> fetchLatestRelease() throws VersionManifestException {
        return fetchVersion(fetchManifest().latest().release());
    }

    @Override
    public Optional<VersionManifestEntry> fetchLatestSnapshot() throws VersionManifestException {
        return fetchVersion(fetchManifest().latest().snapshot());
    }

    private VersionMetadata findMetadataFromCache(String versionId) throws VersionManifestException {
        Path versionFile = gameDirectory.resolve(".minecraft").resolve("versions").resolve(versionId).resolve(versionId + ".json");
        if (versionFile.toFile().exists()) {
            try {
                String json = Files.readString(versionFile);
                JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
                return VersionMetadataParser.parse(jsonObject);
            } catch (Exception exception) {
                throw new VersionManifestException("Failed to read version metadata from cache for version: " + versionId, exception);
            }
        }

        return null;
    }

    @Override
    public VersionMetadata fetchVersionMetadata(String versionId) throws VersionManifestException {
        if (versionId == null || versionId.isEmpty())
            throw new VersionManifestException("Version ID cannot be null or empty");

        VersionMetadata cachedMetadata = findMetadataFromCache(versionId);
        if (cachedMetadata != null)
            return cachedMetadata;

        VersionManifestEntry entry = fetchVersion(versionId)
                .orElseThrow(() -> new VersionManifestException("Version not found: " + versionId));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(entry.url())
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new VersionManifestException("Failed to fetch version metadata: HTTP " + response.statusCode());

            String responseBody = response.body();
            if (responseBody == null || responseBody.isEmpty())
                throw new VersionManifestException("Failed to fetch version metadata: Empty response body");

            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
            return VersionMetadataParser.parse(json);
        } catch (Exception exception) {
            throw new VersionManifestException("Failed to fetch version metadata for version: " + versionId, exception);
        }
    }

    @Override
    public VersionMetadata fetchVersionMetadata(VersionManifestEntry entry) throws VersionManifestException {
        return fetchVersionMetadata(entry.id());
    }

    @Override
    public void saveVersionMetadata(VersionMetadata metadata) {
        try {
            Path versionFile = gameDirectory.resolve(".minecraft").resolve("versions").resolve(metadata.id()).resolve(metadata.id() + ".json");
            String json = GSON.toJson(metadata);
            Files.writeString(versionFile, json);
        } catch (Exception exception) {
            throw new VersionManifestException("Failed to save version metadata for version: " + metadata.id(), exception);
        }
    }
}
