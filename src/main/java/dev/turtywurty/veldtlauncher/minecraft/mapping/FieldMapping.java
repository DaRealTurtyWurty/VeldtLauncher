package dev.turtywurty.veldtlauncher.minecraft.mapping;

public record FieldMapping(
        String obfuscatedName,
        String deobfuscatedName,
        String type
) {
}
