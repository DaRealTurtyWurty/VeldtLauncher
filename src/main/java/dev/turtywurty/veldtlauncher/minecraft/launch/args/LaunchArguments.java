package dev.turtywurty.veldtlauncher.minecraft.launch.args;

import java.util.List;

public record LaunchArguments(
        List<String> jvmArguments,
        List<String> gameArguments
) {}