package dev.turtywurty.veldtlauncher.minecraft.java;

import dev.turtywurty.veldtlauncher.minecraft.metadata.model.JavaVersion;

import java.util.Optional;

public interface JavaDiscoveryService {
    Optional<JDK> findJavaExecutable(JavaVersion version) throws JavaDiscoveryException;
}
