package dev.turtywurty.veldtlauncher.ui.auth;

import javafx.scene.image.*;

public final class SkinAvatarView extends ImageView {
    private static final int SKIN_FACE_SIZE = 8;
    private static final int FACE_X = 8;
    private static final int FACE_Y = 8;
    private static final int HAT_X = 40;
    private static final int HAT_Y = 8;

    private SkinAvatarView(WritableImage avatarImage) {
        super(avatarImage);
        setSmooth(false);
    }

    public static SkinAvatarView create(String skinUrl, int avatarSize) {
        if (avatarSize <= 0 || avatarSize % SKIN_FACE_SIZE != 0)
            throw new IllegalArgumentException("avatarSize must be positive and divisible by " + SKIN_FACE_SIZE);

        var image = new Image(skinUrl, false);
        if (image.isError())
            return null;

        PixelReader reader = image.getPixelReader();
        if (reader == null)
            return null;

        int scale = avatarSize / SKIN_FACE_SIZE;
        var avatarImage = new WritableImage(avatarSize, avatarSize);
        PixelWriter writer = avatarImage.getPixelWriter();
        for (int y = 0; y < SKIN_FACE_SIZE; y++) {
            for (int x = 0; x < SKIN_FACE_SIZE; x++) {
                int faceArgb = reader.getArgb(FACE_X + x, FACE_Y + y);
                int hatArgb = reader.getArgb(HAT_X + x, HAT_Y + y);
                int combinedArgb = blendArgb(faceArgb, hatArgb);
                writeScaledPixel(writer, x, y, scale, combinedArgb);
            }
        }

        return new SkinAvatarView(avatarImage);
    }

    private static void writeScaledPixel(PixelWriter writer, int sourceX, int sourceY, int scale, int argb) {
        int scaledX = sourceX * scale;
        int scaledY = sourceY * scale;
        for (int y = 0; y < scale; y++) {
            for (int x = 0; x < scale; x++) {
                writer.setArgb(scaledX + x, scaledY + y, argb);
            }
        }
    }

    private static int blendArgb(int baseArgb, int overlayArgb) {
        int overlayAlpha = (overlayArgb >>> 24) & 0xFF;
        if (overlayAlpha == 0)
            return baseArgb;

        if (overlayAlpha == 255)
            return overlayArgb;

        int baseAlpha = (baseArgb >>> 24) & 0xFF;
        double overlayAlphaNormalized = overlayAlpha / 255.0;
        double baseAlphaNormalized = baseAlpha / 255.0;
        double outAlpha = overlayAlphaNormalized + (baseAlphaNormalized * (1.0 - overlayAlphaNormalized));
        if (outAlpha <= 0.0)
            return 0;

        int red = blendChannel((baseArgb >> 16) & 0xFF, (overlayArgb >> 16) & 0xFF,
                baseAlphaNormalized, overlayAlphaNormalized, outAlpha);
        int green = blendChannel((baseArgb >> 8) & 0xFF, (overlayArgb >> 8) & 0xFF,
                baseAlphaNormalized, overlayAlphaNormalized, outAlpha);
        int blue = blendChannel(baseArgb & 0xFF, overlayArgb & 0xFF,
                baseAlphaNormalized, overlayAlphaNormalized, outAlpha);
        int alpha = (int) Math.round(outAlpha * 255.0);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int blendChannel(int base, int overlay, double baseAlpha, double overlayAlpha, double outAlpha) {
        double value = ((overlay * overlayAlpha) + (base * baseAlpha * (1.0 - overlayAlpha))) / outAlpha;
        return (int) Math.round(value);
    }
}
