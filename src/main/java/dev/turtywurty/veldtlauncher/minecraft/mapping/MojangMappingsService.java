package dev.turtywurty.veldtlauncher.minecraft.mapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class MojangMappingsService implements MappingsService {
    public static final MojangMappingsService INSTANCE = new MojangMappingsService();
    private final MappingsParser parser;
    private final Map<Path, Mappings> cache = new ConcurrentHashMap<>();

    public MojangMappingsService() {
        this(MojangMappingsParser.INSTANCE);
    }

    public MojangMappingsService(MappingsParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    @Override
    public Mappings load(Path gameDirectory, String versionId) throws MappingsException {
        if (gameDirectory == null)
            throw new MappingsException("Game directory cannot be null.");

        if (versionId == null || versionId.isBlank())
            throw new MappingsException("Version id cannot be null or blank.");

        Path mappingsPath = resolveMappingsPath(gameDirectory, versionId);
        if (Files.notExists(mappingsPath))
            throw new MappingsException("Mappings file does not exist: " + mappingsPath);

        if (!Files.isRegularFile(mappingsPath))
            throw new MappingsException("Mappings path is not a file: " + mappingsPath);

        if (!Files.isReadable(mappingsPath))
            throw new MappingsException("Mappings file is not readable: " + mappingsPath);

        return this.cache.computeIfAbsent(mappingsPath.toAbsolutePath().normalize(), path -> {
            try {
                return this.parser.parse(path);
            } catch (MappingsParseException exception) {
                throw new MappingsException("Failed to parse mappings file: " + path, exception);
            }
        });
    }

    private Path resolveMappingsPath(Path gameDirectory, String versionId) {
        return gameDirectory.resolve(".minecraft")
                .resolve("versions")
                .resolve(versionId)
                .resolve(versionId + ".mappings.txt");
    }
}
