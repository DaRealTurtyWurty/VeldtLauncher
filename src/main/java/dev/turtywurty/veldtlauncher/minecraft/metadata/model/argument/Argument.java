package dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument;

import java.util.List;

public interface Argument {
    String getValue();

    List<ArgumentRule> getRules();
}
