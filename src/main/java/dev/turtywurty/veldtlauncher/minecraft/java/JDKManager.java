package dev.turtywurty.veldtlauncher.minecraft.java;

import dev.turtywurty.veldtlauncher.minecraft.metadata.model.JavaVersion;
import dev.turtywurty.veldtlauncher.util.OperatingSystem;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Locates locally installed JDK distributions across supported platforms and caches the results
 * for quick lookup. Provides helpers to refresh the cache and query the discovered installations.
 */
public class JDKManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(JDKManager.class);
    /**
     * The name of the Java executable, platform-dependent (e.g., "java.exe" on Windows, "java" on Linux/macOS).
     */
    public static final String JAVA_EXECUTABLE_NAME = OperatingSystem.CURRENT == OperatingSystem.WINDOWS ? "java.exe" : "java";

    private static final List<String> WIN_JDK_PATHS = List.of(
            "{drive}:\\Program Files\\Java",
            "{drive}:\\Program Files (x86)\\Java",
            "{drive}:\\Program Files\\Eclipse Adoptium",
            "{drive}:\\Program Files (x86)\\Eclipse Adoptium",
            "{drive}:\\Program Files\\Azul",
            "{drive}:\\Program Files (x86)\\Azul",
            "{drive}:\\Program Files\\Zulu",
            "{drive}:\\Program Files (x86)\\Zulu",
            "{drive}:\\Program Files\\Amazon Corretto",
            "{drive}:\\Program Files (x86)\\Amazon Corretto",
            "{drive}:\\Program Files\\BellSoft",
            "{drive}:\\Program Files\\GraalVM"
    );
    private static final List<String> LINUX_JDK_PATHS = List.of(
            "/usr/lib/jvm",
            "/usr/java",
            "/opt/java",
            "/opt/jdk"
    );
    private static final List<JDK> JDKS = new CopyOnWriteArrayList<>();

    /**
     * Returns all discovered JDKs from the most recent scan.
     *
     * @return cached, unmodifiable list of JDK descriptors
     */
    public static List<JDK> getAvailableJDKs() {
        return Collections.unmodifiableList(JDKS);
    }

    /**
     * Retrieves the first discovered JDK from the cached list, or {@code null} if no JDKs are found.
     *
     * @return first available JDK or {@code null} when none are detected
     */
    public static @Nullable JDK getDefaultJDK() {
        List<JDK> jdks = getAvailableJDKs();
        if (jdks.isEmpty())
            return null;

        return jdks.getFirst();
    }

    /**
     * Re-scans all known sources (environment variables, PATH entries, and configured directories)
     * to rebuild the cached list of JDK installations.
     */
    public static void refreshJDKs() {
        JDKS.clear();
        JDKS.addAll(discoverJDKs());

        for (JDK jdk : JDKS) {
            LOGGER.debug("Detected JDK: {} (brand: {}, version: {}, path: {})",
                    jdk.name(), jdk.brand(), jdk.version(), jdk.path());
        }
    }

    /**
     * Filters cached JDKs to only those whose version falls within the provided range.
     *
     * @param minVersion inclusive lower bound; pass {@code null} to ignore the lower bound
     * @param maxVersion inclusive upper bound; pass {@code null} to ignore the upper bound
     * @return list of JDKs whose {@link JavaVersion} is within the requested bounds
     */
    public static List<JDK> getJDKsInVersionRange(JavaVersion minVersion, JavaVersion maxVersion) {
        List<JDK> filtered = new ArrayList<>();
        for (JDK jdk : JDKS) {
            if ((minVersion == null || jdk.version().compareTo(minVersion) >= 0) &&
                    (maxVersion == null || jdk.version().compareTo(maxVersion) <= 0)) {
                filtered.add(jdk);
            }
        }

        return filtered;
    }

    /**
     * Scans every configured source of JDK installations and produces a deduplicated list.
     *
     * @return list of discovered {@link JDK} descriptors
     */
    private static List<JDK> discoverJDKs() {
        // Location 1a: JAVA_HOME environment variable
        List<JDK> jdks = new ArrayList<>();
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isEmpty()) {
            addIfValid(jdks, JDKUtils.createJDKFromAnyPath(javaHome));
        }

        // Location 1b: JDK_HOME environment variable
        String jdkHome = System.getenv("JDK_HOME");
        if (jdkHome != null && !jdkHome.isEmpty()) {
            addIfValid(jdks, JDKUtils.createJDKFromAnyPath(jdkHome));
        }

        // Location 2: System PATH
        String javaPath = JDKUtils.findJavaOnPath();
        if (javaPath != null) {
            addIfValid(jdks, JDKUtils.createJDKFromAnyPath(javaPath));
        }

        // Location 3: Common installation directories
        discoverJDKsInCommonDirectories(jdks);

        return removeDuplicateJDKs(jdks);
    }

    /**
     * Traverses known installation directories (SDKMAN, vendor folders, etc.) and registers any
     * detected JDK executables while respecting the provided exclusion list.
     *
     * @param jdks collection being populated with discovered JDKs
     */
    private static void discoverJDKsInCommonDirectories(List<JDK> jdks) {
        for (Path dir : getPossibleJDKPaths()) {
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                    for (Path entry : stream) {
                        if (!Files.isDirectory(entry))
                            continue;

                        Path exe;
                        if (OperatingSystem.CURRENT == OperatingSystem.MAC) {
                            // Try bundle layout first
                            Path bundle = entry.resolve("Contents").resolve("Home").resolve("bin").resolve(JAVA_EXECUTABLE_NAME);
                            // Fallback: plain folder layout (SDKMAN/asdf/.gradle/jdks, etc.)
                            Path flat = entry.resolve("bin").resolve(JAVA_EXECUTABLE_NAME);
                            exe = Files.isExecutable(bundle) ? bundle : flat;
                        } else {
                            exe = entry.resolve("bin").resolve(JAVA_EXECUTABLE_NAME);
                        }

                        addIfValid(jdks, JDKUtils.createJDKFromAnyPath(exe.toString()));
                    }
                } catch (IOException exception) {
                    LOGGER.warn("Failed to read JDKs from directory: {}", dir, exception);
                }
            }
        }
    }

    /**
     * Adds the supplied JDK to the running list when it is non-null and not excluded.
     *
     * @param jdks aggregate list being populated
     * @param jdk  potential discovery result
     */
    private static void addIfValid(List<JDK> jdks, JDK jdk) {
        if (jdk == null)
            return;

        jdks.add(jdk);
    }

    /**
     * Consolidates the provided discoveries by normalising their paths and removing duplicates.
     *
     * @param jdks discovered JDK entries
     * @return list with duplicate locations removed
     */
    private static List<JDK> removeDuplicateJDKs(List<JDK> jdks) {
        Map<String, JDK> uniqueJDKs = new LinkedHashMap<>();
        for (JDK jdk : jdks) {
            try {
                Path normalizedPath = jdk.path().toRealPath();
                uniqueJDKs.putIfAbsent(normalizedPath.toString(), new JDK(normalizedPath, jdk.name(), jdk.version()));
            } catch (IOException | InvalidPathException ignored) {
                uniqueJDKs.putIfAbsent(jdk.path().toAbsolutePath().normalize().toString(), jdk);
            }
        }

        return new ArrayList<>(uniqueJDKs.values());
    }

    /**
     * Produces a platform-aware list of default directories that commonly contain JDK installations,
     * augmented with user-level managers such as SDKMAN and Gradle caches.
     *
     * @return candidate directories to probe for JDKs
     */
    private static List<Path> getPossibleJDKPaths() {
        Set<Path> candidates = new LinkedHashSet<>();

        switch (OperatingSystem.CURRENT) {
            case WINDOWS -> {
                for (String basePath : WIN_JDK_PATHS) {
                    for (char drive = 'C'; drive <= 'Z'; drive++) { // Start from C, A/B are usually floppy
                        Path driveRoot = Path.of(drive + ":\\");
                        if (Files.exists(driveRoot)) {
                            String path = basePath.replace("{drive}", String.valueOf(drive));
                            candidates.add(normalizePath(Path.of(path)));
                        }
                    }
                }
            }
            case MAC -> {
                // System-wide JDK bundles
                candidates.add(normalizePath(Path.of("/Library/Java/JavaVirtualMachines")));

                // User-scoped JDK bundles (JetBrains downloader often ends up here)
                String home = System.getProperty("user.home");
                if (home != null && !home.isBlank()) {
                    candidates.add(normalizePath(Path.of(home, "Library", "Java", "JavaVirtualMachines")));
                }

                // Homebrew (Apple Silicon + Intel)
                candidates.add(normalizePath(Path.of("/opt/homebrew/opt")));   // ARM
                candidates.add(normalizePath(Path.of("/usr/local/opt")));      // Intel
            }
            case LINUX -> LINUX_JDK_PATHS.forEach(path -> candidates.add(normalizePath(Path.of(path))));
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            candidates.add(normalizePath(Path.of(userHome, ".sdkman", "candidates", "java")));
            candidates.add(normalizePath(Path.of(userHome, ".asdf", "installs", "java")));
            candidates.add(normalizePath(Path.of(userHome, ".jdks")));
            candidates.add(normalizePath(Path.of(userHome, ".gradle", "jdks")));
        }

        String gradleUserHome = System.getenv("GRADLE_USER_HOME");
        if (gradleUserHome != null && !gradleUserHome.isBlank()) {
            candidates.add(normalizePath(Path.of(gradleUserHome, "jdks")));
        }

        return new ArrayList<>(candidates);
    }

    /**
     * Normalises a path by converting it to an absolute path.
     *
     * @param path the path to normalise
     * @return the normalised path
     */
    public static Path normalizePath(Path path) {
        if (path == null)
            return null;

        return path.toAbsolutePath().normalize();
    }
}