package dev.turtywurty.veldtlauncher.minecraft.java;

import dev.turtywurty.veldtlauncher.minecraft.metadata.model.JavaVersion;

import java.util.List;
import java.util.Optional;

public class DefaultJavaDiscoveryService implements JavaDiscoveryService {
    @Override
    public Optional<JDK> findJavaExecutable(JavaVersion version) throws JavaDiscoveryException {
        if (version == null)
            throw new JavaDiscoveryException("Java version cannot be null");

        try {
            List<JDK> availableJDKs = JDKManager.getAvailableJDKs();
            if (availableJDKs.isEmpty()) {
                JDKManager.refreshJDKs();
                availableJDKs = JDKManager.getAvailableJDKs();
            }

            int majorVersion = version.majorVersion();
            return availableJDKs.stream()
                    .filter(jdk -> jdk.version() != null && jdk.version().majorVersion() == majorVersion)
                    .findFirst();
        } catch (JavaDiscoveryException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JavaDiscoveryException("Failed to discover Java executable for version " + version, exception);
        }
    }
}
