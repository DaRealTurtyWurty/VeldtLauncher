package dev.turtywurty.veldtlauncher.util;

import io.nayuki.qrcodegen.QrCode;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.util.Objects;

public final class QrCodeUtil {
    private QrCodeUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Generates a QR Code from the given text and error correction level.
     *
     * @param text                 the text to encode in the QR Code
     * @param errorCorrectionLevel the error correction level to use for the QR Code
     * @return a QrCode object representing the generated QR Code
     */
    public static QrCode generateQrCode(String text, QrCode.Ecc errorCorrectionLevel) {
        return QrCode.encodeText(text, errorCorrectionLevel);
    }

    /**
     * Returns a string of the given QR Code, using Unicode block characters to represent the modules.
     * Dark modules are shown as "██" and light modules are shown as "  " (two spaces). The string is
     * terminated with a newline character and uses multiple lines to represent the rows of the QR Code.
     *
     * @param qrCode the QR Code to convert to a string
     * @return a string representation of the QR Code, using "██" for dark modules and "  " for light modules, with newlines separating rows
     */
    public static String qrCodeToString(QrCode qrCode) {
        var sb = new StringBuilder();
        for (int y = 0; y < qrCode.size; y++) {
            for (int x = 0; x < qrCode.size; x++) {
                sb.append(qrCode.getModule(x, y) ? "██" : "  ");
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Returns a raster image depicting the specified QR Code, with
     * the specified module scale, border modules, and module colours.
     * <p>For example, scale=10 and border=4 means to pad the QR Code with 4 light border
     * modules on all four sides, and use 10&#xD7;10 pixels to represent each module.
     *
     * @param qr         the QR Code to render (not {@code null})
     * @param scale      the side length (measured in pixels, must be positive) of each module
     * @param border     the number of border modules to add, which must be non-negative
     * @param lightColor the colour to use for light modules, in 0xRRGGBB format
     * @param darkColor  the colour to use for dark modules, in 0xRRGGBB format
     * @return a new image representing the QR Code, with padding and scaling
     * @throws NullPointerException     if the QR Code is {@code null}
     * @throws IllegalArgumentException if the scale or border is out of range, or if
     *                                  {scale, border, size} cause the image dimensions to exceed Integer.MAX_VALUE
     */
    public static BufferedImage toImage(QrCode qr, int scale, int border, int lightColor, int darkColor) {
        Objects.requireNonNull(qr);
        if (scale <= 0 || border < 0)
            throw new IllegalArgumentException("Value out of range");

        if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale)
            throw new IllegalArgumentException("Scale or border too large");

        BufferedImage result = new BufferedImage((qr.size + border * 2) * scale, (qr.size + border * 2) * scale, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                boolean color = qr.getModule(x / scale - border, y / scale - border);
                result.setRGB(x, y, color ? darkColor : lightColor);
            }
        }

        return result;
    }

    /**
     * A convenience overload of {@link #toImage(QrCode, int, int, int, int)} that uses white as the light module colour
     * and black as the dark module colour.
     *
     * @param qr     the QR Code to render (not {@code null})
     * @param scale  the side length (measured in pixels, must be positive) of each module
     * @param border the number of border modules to add, which must be non-negative
     * @return a new image representing the QR Code, with padding and scaling, using white for light modules and black for dark modules
     * @throws NullPointerException     if the QR Code is {@code null}
     * @throws IllegalArgumentException if the scale or border is out of range, or if {scale, border, size} cause the image dimensions to exceed Integer.MAX_VALUE
     */
    public static BufferedImage toImage(QrCode qr, int scale, int border) {
        return toImage(qr, scale, border, 0xFFFFFF, 0x000000);
    }

    /**
     * A convenience overload of {@link #toImage(QrCode, int, int, int, int)} that converts the resulting image to a JavaFX Image.
     *
     * @param qr     the QR Code to render (not {@code null})
     * @param scale  the side length (measured in pixels, must be positive) of each module
     * @param border the number of border modules to add, which must be non-negative
     * @return a new JavaFX Image representing the QR Code, with padding and scaling, using white for light modules and black for dark modules
     * @throws NullPointerException     if the QR Code is {@code null}
     * @throws IllegalArgumentException if the scale or border is out of range, or if {scale, border, size} cause the image dimensions to exceed Integer.MAX_VALUE
     */
    public static Image toFxImage(QrCode qr, int scale, int border) {
        return SwingFXUtils.toFXImage(toImage(qr, scale, border), null);
    }
}
