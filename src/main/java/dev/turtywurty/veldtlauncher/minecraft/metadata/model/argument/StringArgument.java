package dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument;

import java.util.List;

public record StringArgument(String value) implements Argument {
    @Override
    public String getValue() {
        return value;
    }

    @Override
    public List<ArgumentRule> getRules() {
        return List.of();
    }
}
