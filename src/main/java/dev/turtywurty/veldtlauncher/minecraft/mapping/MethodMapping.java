package dev.turtywurty.veldtlauncher.minecraft.mapping;

public record MethodMapping(
        String obfuscatedName,
        String deobfuscatedName,
        String returnType,
        String[] parameterTypes,
        int startLine,
        int endLine
) {
}
