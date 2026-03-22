package dev.turtywurty.veldtlauncher.minecraft.java;

import dev.turtywurty.veldtlauncher.minecraft.metadata.model.JavaVersion;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * A descriptor for a Java Development Kit (JDK) installation.
 * This class encapsulates information about a JDK, including its installation path, name,
 * Java version, and brand (e.g., Oracle, Adoptium).
 */
public record JDK(Path path, String name, JavaVersion version, Brand brand) {
    /**
     * Constructs a new {@code JDK} instance with the specified path, name, version, and brand.
     *
     * @param path    The absolute path to the JDK installation directory.
     * @param name    The display name of the JDK.
     * @param version The Java version of this JDK.
     * @param brand   The brand of this JDK (e.g., Oracle, Adoptium).
     */
    public JDK {
    }

    /**
     * Constructs a new {@code JDK} instance with the specified path, name, and version.
     * The brand will be automatically determined based on the JDK's properties.
     *
     * @param path    The absolute path to the JDK installation directory.
     * @param name    The display name of the JDK.
     * @param version The Java version of this JDK.
     */
    public JDK(Path path, String name, JavaVersion version) {
        this(path, name, version, Brand.from(path, name));
    }

    @Override
    public @NotNull String toString() {
        return "JDK{" +
                "path=" + path +
                ", name='" + name + '\'' +
                ", version=" + version +
                ", brand=" + brand +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass())
            return false;

        JDK jdk = (JDK) other;
        return Objects.equals(path, jdk.path)
                && Objects.equals(name, jdk.name)
                && Objects.equals(version, jdk.version)
                && brand == jdk.brand;
    }

    /**
     * Returns the absolute path to the JDK installation directory.
     *
     * @return The path to the JDK.
     */
    @Override
    public Path path() {
        return path;
    }

    /**
     * Returns the display name of this JDK.
     *
     * @return The name of the JDK.
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the Java version of this JDK.
     *
     * @return The Java version.
     */
    @Override
    public JavaVersion version() {
        return version;
    }

    /**
     * Returns the brand of this JDK.
     *
     * @return The JDK brand.
     */
    @Override
    public Brand brand() {
        return brand;
    }

    /**
     * Resolves the absolute path to a tool executable within this JDK installation.
     *
     * @param executableName filename of the CLI tool (e.g. {@code jar} or {@code keytool})
     * @return absolute path to the executable, best-effort even when this JDK descriptor points directly to {@code bin}
     */
    public Path executablePath(String executableName) {
        Objects.requireNonNull(executableName, "Executable name cannot be null");
        Path binDirectory = path.resolve("bin");
        Path candidate = binDirectory.resolve(executableName);
        if (Files.exists(candidate))
            return candidate;

        return path.resolve(executableName);
    }

    /**
     * Represents the brand or vendor of a JDK distribution.
     */
    public enum Brand {
        ORACLE("oracle"),
        ADOPTIUM("temurin", "adoptopenjdk", "adoptium", "eclipse"),
        AZUL("zulu", "azul"),
        CORRETTO("corretto", "amazon"),
        BELL_SOFT("liberica", "bellsoft"),
        GRAAL("graalvm", "graal"),
        SAP("sapmachine", "sap"),
        RED_HAT("redhat", "red hat", "rhel"),
        MICROSOFT("microsoft", "ms"),
        IBM("ibm", "semeru"),
        UNKNOWN("java");

        private final String key;
        private final String[] aliases;
        private String imagePath;

        /**
         * Constructs a Brand with a key, and optional aliases.
         *
         * @param key     The primary identifier for the brand.
         * @param aliases Additional names or identifiers for the brand.
         */
        Brand(String key, String... aliases) {
            this.key = key;
            this.aliases = aliases;
        }

        /**
         * Determines the {@code Brand} of a given JDK based on its properties and name.
         * It first attempts to resolve the brand from the JDK's release properties (IMPLEMENTOR, VENDOR),
         * then falls back to string matching on the JDK's name and path if properties are inconclusive.
         *
         * @param jdkPath The absolute path to the JDK installation directory.
         * @param jdkName The display name of the JDK.
         * @return The resolved {@code Brand}, or {@code UNKNOWN} if no specific brand can be identified.
         */
        private static Brand from(Path jdkPath, String jdkName) {
            String path = jdkPath.toAbsolutePath().toString().toLowerCase(Locale.ROOT);
            String name = (jdkName == null ? "" : jdkName).toLowerCase(Locale.ROOT);
            Properties props = JDKUtils.readReleaseProperties(jdkPath);
            String implVendor = props.getProperty("IMPLEMENTOR", "").toLowerCase(Locale.ROOT);
            String vendor = props.getProperty("VENDOR", "").toLowerCase(Locale.ROOT);
            for (Brand brand : values()) {
                if (brand == UNKNOWN)
                    continue;

                if (implVendor.contains(brand.key) || vendor.contains(brand.key))
                    return brand;

                if (brand.aliases != null) {
                    for (String alias : brand.aliases) {
                        if (implVendor.contains(alias) || vendor.contains(alias))
                            return brand;
                    }
                }
            }

            // If release properties are inconclusive, fall back to string matching on name and path
            for (Brand brand : values()) {
                if (brand == UNKNOWN)
                    continue;

                if (name.contains(brand.key) || path.contains(brand.key))
                    return brand;

                if (brand.aliases != null) {
                    for (String alias : brand.aliases) {
                        if (name.contains(alias) || path.contains(alias))
                            return brand;
                    }
                }
            }

            return UNKNOWN;
        }
    }
}
