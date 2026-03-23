package dev.turtywurty.veldtlauncher.minecraft.mapping;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public interface MappingsParser {
    Mappings parse(String mappings) throws MappingsParseException;

    default Mappings parse(byte[] mappings) throws MappingsParseException {
        return parse(new String(mappings, StandardCharsets.UTF_8));
    }

    default Mappings parse(Path mappingsFile) throws MappingsParseException {
        try {
            return parse(Files.readString(mappingsFile));
        } catch (Exception exception) {
            throw new MappingsParseException("Failed to read mappings file: " + mappingsFile, exception);
        }
    }
}
