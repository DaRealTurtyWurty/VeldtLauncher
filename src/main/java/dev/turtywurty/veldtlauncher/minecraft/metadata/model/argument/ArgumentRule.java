package dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument;

import java.util.Map;

public record ArgumentRule(
        String action,
        Map<String, Boolean> features
) {
}
