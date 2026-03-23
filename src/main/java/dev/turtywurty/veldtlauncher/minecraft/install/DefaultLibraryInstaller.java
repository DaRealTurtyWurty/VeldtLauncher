package dev.turtywurty.veldtlauncher.minecraft.install;

import dev.turtywurty.veldtlauncher.instance.play.InstancePlayReporter;
import dev.turtywurty.veldtlauncher.instance.play.InstancePlayStep;
import dev.turtywurty.veldtlauncher.minecraft.metadata.VersionMetadata;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.Extract;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.Library;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.Native;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.Rule;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.download.Artifact;
import dev.turtywurty.veldtlauncher.minecraft.metadata.model.download.LibraryDownloads;
import dev.turtywurty.veldtlauncher.util.DownloadUtil;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DefaultLibraryInstaller implements LibraryInstaller {
    private static final URI DEFAULT_LIBRARY_BASE_URI = URI.create("https://libraries.minecraft.net/");
    private final InstancePlayReporter reporter;

    public DefaultLibraryInstaller() {
        this(InstancePlayReporter.noOp());
    }

    public DefaultLibraryInstaller(InstancePlayReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public Path installLibraries(VersionMetadata metadata, Path gameDirectory) throws VersionInstallException {
        List<Library> libraries = metadata.libraries();
        if (libraries == null)
            throw new VersionInstallException("Library information is missing for version " + metadata.id());

        Path librariesDirectory = gameDirectory.resolve(".minecraft").resolve("libraries");
        Path nativesDirectory = gameDirectory.resolve(".minecraft").resolve("versions").resolve(metadata.id()).resolve("natives");
        this.reporter.progress(InstancePlayStep.INSTALLING_LIBRARIES, "Preparing libraries", 0, libraries.size());
        int completed = 0;
        for (Library library : libraries) {
            installLibrary(library, librariesDirectory, nativesDirectory);
            completed++;
            String libraryName = library == null || library.name() == null ? "library" : library.name();
            this.reporter.progress(InstancePlayStep.INSTALLING_LIBRARIES, "Processed " + libraryName, completed, libraries.size());
        }

        return librariesDirectory;
    }

    private void installLibrary(Library library, Path librariesDirectory, Path nativesDirectory) throws VersionInstallException {
        List<Rule> rules = library.rules();
        if (rules != null) {
            for (Rule rule : rules) {
                if (!rule.appliesToCurrentEnvironment())
                    return;
            }
        }

        LibraryDownloads downloads = library.downloads();
        Artifact artifact = resolveLibraryArtifact(library, downloads);
        if (artifact != null)
            downloadArtifact(library, artifact, librariesDirectory);

        Artifact nativeArtifact = resolveNativeArtifact(library, downloads);
        if (nativeArtifact == null)
            return;

        Path nativeArchivePath = downloadArtifact(library, nativeArtifact, librariesDirectory);
        extractNativeArchive(nativeArchivePath, nativesDirectory, library.extract(), library.name());
    }

    private Artifact resolveLibraryArtifact(Library library, LibraryDownloads downloads) throws VersionInstallException {
        if (downloads == null)
            return createFallbackArtifact(library, null);

        Artifact artifact = downloads.artifact();
        if (artifact != null)
            return withResolvedUri(library, artifact, null);

        return createFallbackArtifact(library, null);
    }

    private Artifact resolveNativeArtifact(Library library, LibraryDownloads downloads) throws VersionInstallException {
        Native natives = library.natives();
        if (natives == null)
            return null;

        String classifier = natives.classifierForCurrentEnvironment();
        if (classifier == null || classifier.isBlank())
            return null;

        Map<String, Artifact> classifiers = downloads == null ? Map.of() : downloads.classifiers();
        Artifact nativeArtifact = classifiers.get(classifier);
        if (nativeArtifact != null)
            return withResolvedUri(library, nativeArtifact, classifier);

        return createFallbackArtifact(library, classifier);
    }

    private Artifact createFallbackArtifact(Library library, String classifier) throws VersionInstallException {
        String artifactPath = buildArtifactPath(library.name(), classifier);
        URI uri = resolveArtifactUri(library, artifactPath);
        return new Artifact(artifactPath, null, 0L, uri);
    }

    private Artifact withResolvedUri(Library library, Artifact artifact, String classifier) throws VersionInstallException {
        if (artifact.uri() != null)
            return artifact;

        String artifactPath = artifact.path();
        if (artifactPath == null || artifactPath.isBlank()) {
            if (classifier == null)
                return artifact;

            artifactPath = buildArtifactPath(library.name(), classifier);
        }

        return new Artifact(
                artifactPath,
                artifact.sha1(),
                artifact.size(),
                resolveArtifactUri(library, artifactPath)
        );
    }

    private String buildArtifactPath(String coordinate, String classifier) throws VersionInstallException {
        String[] parts = coordinate.split(":");
        if (parts.length < 3 || parts.length > 4)
            throw new VersionInstallException("Invalid library coordinate " + coordinate);

        String groupPath = parts[0].replace('.', '/');
        String artifactId = parts[1];
        String version = parts[2];
        String declaredClassifier = parts.length == 4 ? parts[3] : null;
        String effectiveClassifier = classifier != null && !classifier.isBlank() ? classifier : declaredClassifier;

        String fileName = artifactId + "-" + version;
        if (effectiveClassifier != null && !effectiveClassifier.isBlank())
            fileName += "-" + effectiveClassifier;

        fileName += ".jar";
        return groupPath + "/" + artifactId + "/" + version + "/" + fileName;
    }

    private URI resolveArtifactUri(Library library, String artifactPath) {
        if (artifactPath == null || artifactPath.isBlank())
            return null;

        URI baseUri = library.url() == null ? DEFAULT_LIBRARY_BASE_URI : library.url();
        String base = baseUri.toString();
        return URI.create((base.endsWith("/") ? base : base + "/") + artifactPath);
    }

    private Path downloadArtifact(Library library, Artifact artifact, Path librariesDirectory) throws VersionInstallException {
        URI url = artifact.uri();
        if (url == null)
            throw new VersionInstallException("Download URL is missing for library " + library.name());

        Path libraryPath = librariesDirectory.resolve(requireArtifactPath(library, artifact));
        Path tempFilePath = libraryPath.resolveSibling(libraryPath.getFileName() + ".tmp");
        try {
            this.reporter.log(InstancePlayStep.INSTALLING_LIBRARIES, "Checking " + library.name());
            Files.createDirectories(Objects.requireNonNull(libraryPath.getParent()));
            if (Files.exists(libraryPath) && isExistingArtifactValid(libraryPath, artifact))
                return libraryPath;

            Files.deleteIfExists(libraryPath);
            Files.deleteIfExists(tempFilePath);

            this.reporter.log(InstancePlayStep.INSTALLING_LIBRARIES, "Downloading " + library.name());
            DownloadUtil.downloadFile(url, tempFilePath);
            validateDownloadedArtifact(tempFilePath, artifact, library.name());
            DownloadUtil.moveFile(tempFilePath, libraryPath);
            return libraryPath;
        } catch (VersionInstallException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new VersionInstallException("Failed to install library " + library.name(), exception);
        } finally {
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException ignored) {
            }
        }
    }

    private String requireArtifactPath(Library library, Artifact artifact) throws VersionInstallException {
        if (artifact.path() != null && !artifact.path().isBlank())
            return artifact.path();

        URI url = artifact.uri();
        if (url == null || url.getPath() == null || url.getPath().isBlank())
            throw new VersionInstallException("Artifact path is missing for library " + library.name());

        String urlPath = url.getPath();
        return urlPath.startsWith("/") ? urlPath.substring(1) : urlPath;
    }

    private boolean isExistingArtifactValid(Path artifactPath, Artifact artifact) throws IOException {
        String sha1 = artifact.sha1();
        if (sha1 != null && !sha1.isBlank() && !DownloadUtil.verifySHA1(artifactPath, sha1))
            return false;

        long size = artifact.size();
        return size <= 0 || DownloadUtil.verifyFileSize(artifactPath, size);
    }

    private void validateDownloadedArtifact(Path artifactPath, Artifact artifact, String libraryName) throws VersionInstallException, IOException {
        String sha1 = artifact.sha1();
        if (sha1 != null && !sha1.isBlank() && !DownloadUtil.verifySHA1(artifactPath, sha1))
            throw new VersionInstallException("Library download SHA-1 hash does not match expected value for library " + libraryName);

        long size = artifact.size();
        if (size > 0 && !DownloadUtil.verifyFileSize(artifactPath, size))
            throw new VersionInstallException("Library download file size does not match expected value for library " + libraryName);
    }

    private void extractNativeArchive(Path archivePath, Path nativesDirectory, Extract extract, String libraryName) throws VersionInstallException {
        try {
            this.reporter.log(InstancePlayStep.INSTALLING_LIBRARIES, "Extracting natives for " + libraryName);
            Files.createDirectories(nativesDirectory);
            try (ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(archivePath))) {
                ZipEntry entry;
                while ((entry = inputStream.getNextEntry()) != null) {
                    if (entry.isDirectory() || shouldExclude(entry.getName(), extract))
                        continue;

                    Path outputPath = nativesDirectory.resolve(entry.getName()).normalize();
                    if (!outputPath.startsWith(nativesDirectory))
                        throw new VersionInstallException("Refusing to extract native outside target directory for library " + libraryName);

                    Files.createDirectories(Objects.requireNonNull(outputPath.getParent()));
                    Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception exception) {
            throw new VersionInstallException("Failed to extract native library " + libraryName, exception);
        }
    }

    private boolean shouldExclude(String entryName, Extract extract) {
        if (entryName == null || entryName.isBlank())
            return true;

        String normalizedName = entryName.replace('\\', '/');
        if (normalizedName.startsWith("META-INF/"))
            return true;

        if (extract == null || extract.exclude() == null)
            return false;

        for (String excludedPrefix : extract.exclude()) {
            if (excludedPrefix == null || excludedPrefix.isBlank())
                continue;

            if (normalizedName.startsWith(excludedPrefix.replace('\\', '/')))
                return true;
        }

        return false;
    }
}
