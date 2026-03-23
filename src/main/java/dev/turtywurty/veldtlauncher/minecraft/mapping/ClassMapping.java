package dev.turtywurty.veldtlauncher.minecraft.mapping;

import java.util.Map;

public record ClassMapping(
        String obfuscatedName,
        String deobfuscatedName,
        Map<String, MethodMapping> methodMappings,
        Map<String, FieldMapping> fieldMappings
) {
    public ClassMapping {
        if (obfuscatedName == null || obfuscatedName.isEmpty())
            throw new IllegalArgumentException("obfuscatedName cannot be null or empty");
        if (deobfuscatedName == null || deobfuscatedName.isEmpty())
            throw new IllegalArgumentException("deobfuscatedName cannot be null or empty");
        if (methodMappings == null)
            throw new IllegalArgumentException("methodMappings cannot be null");
        if (fieldMappings == null)
            throw new IllegalArgumentException("fieldMappings cannot be null");

        methodMappings = Map.copyOf(methodMappings);
        fieldMappings = Map.copyOf(fieldMappings);
    }
}
