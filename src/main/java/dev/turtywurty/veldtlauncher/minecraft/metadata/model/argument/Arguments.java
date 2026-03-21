package dev.turtywurty.veldtlauncher.minecraft.metadata.model.argument;

import java.util.List;

public record Arguments(
        DefaultUserJvmArguments defaultUserJvm,
        List<Argument> game,
        List<Argument> jvm
) {
}
