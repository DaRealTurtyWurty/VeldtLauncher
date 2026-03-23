package dev.turtywurty.veldtlauncher.minecraft.mapping;

import java.nio.file.Path;

public interface MappingsService {
    Mappings load(Path gameDirectory, String versionId) throws MappingsException;

    default Mappings loadOrEmpty(Path gameDirectory, String versionId) throws MappingsException {
        try {
            return load(gameDirectory, versionId);
        } catch (MappingsException exception) {
            return Mappings.empty();
        }
    }
}
