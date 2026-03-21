package dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument;

import java.util.List;

public record RuleArgument(
        String[] value,
        List<ArgumentRule> rules
) implements Argument {
    @Override
    public String getValue() {
        return String.join(" ", value);
    }

    @Override
    public List<ArgumentRule> getRules() {
        return rules;
    }
}
