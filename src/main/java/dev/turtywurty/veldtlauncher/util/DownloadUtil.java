package dev.turtywurty.veldtlauncher.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class DownloadUtil {
    private DownloadUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void downloadFile(URI url, Path destination) throws IOException {
        try (InputStream inputStream = url.toURL().openStream()) {
            Files.createDirectories(destination.getParent());
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static boolean verifySHA1(Path filePath, String expectedSha1) throws IOException {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] actualHashBytes = digest.digest();
            var actualHashBuilder = new StringBuilder();
            for (byte b : actualHashBytes) {
                actualHashBuilder.append(String.format("%02x", b));
            }

            String actualSha1 = actualHashBuilder.toString();
            return actualSha1.equalsIgnoreCase(expectedSha1);
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("SHA-1 algorithm not available", exception);
        }
    }

    public static boolean verifyFileSize(Path filePath, long expectedSize) throws IOException {
        long actualSize = Files.size(filePath);
        return actualSize == expectedSize;
    }

    public static void moveFile(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
