package dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument;

import dev.turtywurty.veldtlauncher.minecraft.metadata.model.Rule;

import java.util.List;

public record DefaultUserJvmArguments(
        List<String> value,
        List<Rule> rules
) {
}
